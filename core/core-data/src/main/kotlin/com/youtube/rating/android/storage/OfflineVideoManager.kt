package com.youtube.rating.android.storage

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.youtube.rating.shared.utils.Logger
import com.youtube.rating.core.file.deleteSafely
import com.youtube.rating.core.file.fileSize
import com.youtube.rating.core.file.resolveDisplayName
import com.youtube.rating.core.file.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import com.youtube.rating.shared.data.OfflineVideoModel
import com.youtube.rating.shared.data.OfflineVideosRepository
import com.youtube.rating.android.utils.BackupTrigger
import com.youtube.rating.android.utils.ChangeType
import java.io.File
import java.io.FileOutputStream
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named

/**
 * Manages offline videos - both downloaded from YouTube and added from device
 * ✅ FIXED: Uses Logger.error() instead of printStackTrace()
 */
class OfflineVideoManager(
    context: Context,
    private val repository: OfflineVideosRepository = GlobalContext.get()
        .get(qualifier = named("sharedOfflineVideosRepo")),
    private val backupTrigger: BackupTrigger = GlobalContext.get().get()
) {
    
    companion object {
        private const val TAG = "OfflineVideoManager"
    }
    
    private val appContext = context.applicationContext
    private val offlineDir: File = File(appContext.filesDir, "offline_videos").apply {
        if (!exists()) mkdirs()
    }

    private val thumbnailDir: File = File(appContext.filesDir, "offline_thumbnails").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Get all offline videos from shared repository (NON-BLOCKING - safe for UI)
     */
    suspend fun getOfflineVideosAsync(): List<OfflineVideo> = try {
        withContext(ioDispatcher) {
            repository.getAllVideos().first().map { model ->
                OfflineVideo(
                    id = model.id,
                    youtubeId = model.youtubeId?.takeIf { it.isNotEmpty() },
                    title = model.title,
                    channelName = model.channelName,
                    localPath = model.localPath,
                    thumbnailPath = model.thumbnailPath?.takeIf { it.isNotEmpty() },
                    thumbnailUrl = model.thumbnailUrl?.takeIf { it.isNotEmpty() },
                    duration = model.duration,
                    fileSize = model.fileSize,
                    isFromDevice = model.youtubeId.isNullOrBlank() && model.id.startsWith("device_"),
                    category = model.category?.takeIf { it.isNotEmpty() },
                    addedAt = model.addedAt
                )
            }
        }
    } catch (e: Exception) {
        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        emptyList()
    }

    /**
     * Add video from device (using content URI)
     */
    suspend fun addVideoFromDevice(
        uri: Uri,
        title: String? = null,
        category: String? = null
    ): OfflineVideo? = withContext(ioDispatcher) {
        var destFile: File? = null
        try {
            val contentResolver = appContext.contentResolver

            // Get file info
            val fileName = uri.resolveDisplayName(appContext) ?: "video_${System.currentTimeMillis()}.mp4"
            val fileSize = (contentResolver.fileSize(uri) ?: 0L).coerceAtLeast(0L)
            val duration = getVideoDuration(uri = uri)
            val displayTitle = title ?: fileName.substringBeforeLast(".")

            // Copy file to app's private storage for reliable access
            val timestamp = System.currentTimeMillis()
            destFile = File(offlineDir, "device_${timestamp}_$fileName")

            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE * 4)
                }
            }

            val video = OfflineVideo(
                id = "device_$timestamp",
                youtubeId = null,
                title = displayTitle,
                channelName = "Local Video",
                localPath = destFile.absolutePath,
                thumbnailPath = null,
                thumbnailUrl = null,
                duration = duration,
                fileSize = if (fileSize > 0) fileSize else destFile.length(),
                isFromDevice = true,
                category = category,
                addedAt = System.currentTimeMillis()
            )

            // Persist using shared repository (already in IO context from suspend function)
            repository.addVideo(OfflineVideoModel(
                    id = video.id,
                    youtubeId = video.youtubeId ?: "",
                    title = video.title,
                    channelName = video.channelName,
                    localPath = video.localPath,
                    thumbnailPath = video.thumbnailPath ?: "",
                    thumbnailUrl = video.thumbnailUrl ?: "",
                    duration = video.duration,
                    fileSize = video.fileSize,
                    category = video.category ?: "",
                    addedAt = video.addedAt
                ))
            // Trigger auto-backup
            backupTrigger.trigger(ChangeType.OFFLINE_VIDEO_ADDED)
            video
        } catch (e: Exception) {
            runCatching { destFile?.takeIf { it.exists() }?.delete() }
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Operation failed", e)
            null
        }
    }

    /**
     * Save downloaded YouTube video
     */
    suspend fun saveDownloadedVideo(
        youtubeId: String,
        title: String,
        channelName: String,
        videoData: ByteArray,
        thumbnailUrl: String?,
        duration: Long,
        category: String?
    ): OfflineVideo? = withContext(ioDispatcher) {
        try {
            val videoFile = File(offlineDir, "yt_${youtubeId}.mp4")
            videoFile.writeBytes(videoData)

            val video = OfflineVideo(
                id = "yt_$youtubeId",
                youtubeId = youtubeId,
                title = title,
                channelName = channelName,
                localPath = videoFile.absolutePath,
                thumbnailPath = null,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                fileSize = videoData.size.toLong(),
                isFromDevice = false,
                category = category,
                addedAt = System.currentTimeMillis()
            )

            saveVideo(video = video)
            // Trigger auto-backup
            backupTrigger.trigger(ChangeType.OFFLINE_VIDEO_ADDED)
            video
        } catch (e: Exception) {
            runCatching {
                val videoFile = File(offlineDir, "yt_${youtubeId}.mp4")
                if (videoFile.exists()) videoFile.delete()
            }
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Operation failed", e)
            null
        }
    }

    /**
     * Save downloaded YouTube video with actual file
     */
    suspend fun saveDownloadedYouTubeVideo(
        youtubeId: String,
        title: String,
        channelName: String,
        localPath: String,
        thumbnailUrl: String?,
        duration: Long,
        fileSize: Long,
        category: String?
    ): OfflineVideo? = withContext(ioDispatcher) {
        try {
            val video = OfflineVideo(
                id = "yt_dl_$youtubeId",
                youtubeId = youtubeId,
                title = title,
                channelName = channelName,
                localPath = localPath,
                thumbnailPath = null,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                fileSize = fileSize,
                isFromDevice = false,
                category = category,
                addedAt = System.currentTimeMillis()
            )

            saveVideo(video = video)
            // Trigger auto-backup
            backupTrigger.trigger(ChangeType.OFFLINE_VIDEO_ADDED)
            video
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Operation failed", e)
            null
        }
    }

    /**
     * Save YouTube video reference for offline (without downloading actual video file)
     * This creates a reference entry that marks video as available offline
     */
    suspend fun saveYouTubeVideoReference(
        youtubeId: String,
        title: String,
        channelName: String,
        thumbnailUrl: String?,
        category: String?
    ): OfflineVideo? = withContext(ioDispatcher) {
        try {
            val video = OfflineVideo(
                id = "yt_ref_$youtubeId",
                youtubeId = youtubeId,
                title = title,
                channelName = channelName,
                localPath = "youtube://$youtubeId", // Placeholder reference
                thumbnailPath = null,
                thumbnailUrl = thumbnailUrl,
                duration = 0L,
                fileSize = 0L,
                isFromDevice = false,
                category = category,
                addedAt = System.currentTimeMillis()
            )

            saveVideo(video = video)
            // Trigger auto-backup
            backupTrigger.trigger(ChangeType.OFFLINE_VIDEO_ADDED)
            video
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Operation failed", e)
            null
        }
    }

    /**
     * Save YouTube stream reference with extracted URL for ExoPlayer playback
     * Stream URL je valjan kratko vrijeme, ali omogućava reprodukciju
     */
    suspend fun saveYouTubeStreamReference(
        youtubeId: String,
        title: String,
        channelName: String,
        streamUrl: String,
        thumbnailUrl: String?,
        duration: Long,
        category: String?
    ): OfflineVideo? = withContext(ioDispatcher) {
        try {
            val video = OfflineVideo(
                id = "yt_stream_$youtubeId",
                youtubeId = youtubeId,
                title = title,
                channelName = channelName,
                localPath = streamUrl, // Stream URL za ExoPlayer
                thumbnailPath = null,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                fileSize = 0L,
                isFromDevice = false,
                category = category,
                addedAt = System.currentTimeMillis()
            )

            saveVideo(video = video)
            // Trigger auto-backup
            backupTrigger.trigger(ChangeType.OFFLINE_VIDEO_ADDED)
            video
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Operation failed", e)
            null
        }
    }

    /**
     * Save video to persistent storage
     */
    private suspend fun saveVideo(video: OfflineVideo) {
        val videos = getOfflineVideosAsync().toMutableList()

        // Remove existing if updating
        videos.removeAll { it.id == video.id || (video.youtubeId != null && it.youtubeId == video.youtubeId) }

        videos.add(video)
        saveVideoList(videos = videos)
    }

    /**
     * Remove offline video
     */
    suspend fun removeVideoAsync(videoId: String): Boolean {
        val videos = getOfflineVideosAsync().toMutableList()
        val video = videos.find { it.id == videoId } ?: return false

        // Remove from list and save to database FIRST (before deleting files)
        videos.removeAll { it.id == videoId }
        try {
            saveVideoList(videos = videos)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Failed to save video list during removal", e)
            return false
        }

        // Only delete files AFTER database update succeeds
        // Delete local file if it's a copied file (not a content URI reference)
        if (!video.localPath.startsWith("content://")) {
            val file = video.localPath.toFile()
            if (file.exists() && !file.deleteSafely()) {
                Logger.error(TAG, "Failed to delete video file: ${video.localPath}")
            }
        }

        // Delete thumbnail if exists
        video.thumbnailPath?.let { path ->
            val thumbFile = path.toFile()
            if (thumbFile.exists() && !thumbFile.deleteSafely()) {
                Logger.error(TAG, "Failed to delete thumbnail file: $path")
            }
        }

        // Trigger auto-backup
        backupTrigger.trigger(ChangeType.OFFLINE_VIDEO_REMOVED)

        return true
    }

    /**
     * Check if video is already saved offline
     */
    suspend fun isVideoOfflineAsync(youtubeId: String): Boolean {
        return getOfflineVideosAsync().any { it.youtubeId == youtubeId }
    }

    /**
     * Get offline video by YouTube ID
     */
    suspend fun getOfflineVideoByYoutubeIdAsync(youtubeId: String): OfflineVideo? {
        return getOfflineVideosAsync().find { it.youtubeId == youtubeId }
    }

    /**
     * Get total storage used by offline videos
     */
    suspend fun getTotalStorageUsedAsync(): Long {
        return getOfflineVideosAsync().sumOf { it.fileSize }
    }

    /**
     * Clear all offline videos
     */
    suspend fun clearAllVideosAsync() = withContext(ioDispatcher) {
        getOfflineVideosAsync().forEach { video ->
            if (!video.localPath.startsWith("content://")) {
                val file = video.localPath.toFile()
                if (file.exists() && !file.deleteSafely()) {
                    Logger.error(TAG, "Failed to delete video file during clear all: ${video.localPath}")
                }
            }
            video.thumbnailPath?.let { path ->
                val thumbFile = path.toFile()
                if (thumbFile.exists() && !thumbFile.deleteSafely()) {
                    Logger.error(TAG, "Failed to delete thumbnail file during clear all: $path")
                }
            }
        }
        repository.clearAll()
        // Trigger auto-backup
        backupTrigger.trigger(ChangeType.OFFLINE_VIDEO_CLEARED)
    }

    /**
     * Update video metadata (title, category)
     */
    suspend fun updateVideoAsync(videoId: String, newTitle: String? = null, newCategory: String? = null): Boolean {
        return try {
            withContext(ioDispatcher) {
                repository.updateVideo(videoId, newTitle, newCategory)
            }
            // Trigger auto-backup
            backupTrigger.trigger(ChangeType.OFFLINE_VIDEO_UPDATED)
            true
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            false
        }
    }

    /**
     * ✅ FIXED: Sigurnija strategija za spremanje liste videa.
     * Umjesto clear-then-add patterna (koji može uzrokovati gubitak podataka pri crashu),
     * koristimo upsert pristup koji minimizira rizik.
     */
    private suspend fun saveVideoList(videos: List<OfflineVideo>) = withContext(ioDispatcher) {
        try {
            // Get existing videos to track what needs to be removed
            val existingVideos = repository.getAllVideos().first()
            val existingIds = existingVideos.map { it.id }.toSet()
            val newIds = videos.map { it.id }.toSet()

            // First, add/update all videos from the new list (upsert)
            videos.forEach { v ->
                repository.addVideo(OfflineVideoModel(
                    id = v.id,
                    youtubeId = v.youtubeId ?: "",
                    title = v.title,
                    channelName = v.channelName,
                    localPath = v.localPath,
                    thumbnailPath = v.thumbnailPath ?: "",
                    thumbnailUrl = v.thumbnailUrl ?: "",
                    duration = v.duration,
                    fileSize = v.fileSize,
                    category = v.category ?: "",
                    addedAt = v.addedAt
                ))
            }

            // Then, remove videos that are no longer in the list
            val idsToRemove = existingIds - newIds
            idsToRemove.forEach { id ->
                try {
                    repository.deleteVideo(id)
                } catch (e: Exception) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    Logger.error(TAG, "Failed to remove video $id during sync", e)
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to save video list", e)
            com.youtube.rating.android.sentry.SentryLogger.captureException(
                e,
                tags = mapOf("where" to "OfflineVideoManager.saveVideoList")
            )
        }
    }

    private fun getVideoDuration(uri: Uri): Long {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            0L
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                // best-effort release
            }
        }
    }

    /**
     * Scan device for videos and return list
     */
    suspend fun scanDeviceVideosAsync(): List<DeviceVideo> = withContext(ioDispatcher) {
        val videos = mutableListOf<DeviceVideo>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Video.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("1000") // At least 1 second
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        appContext.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val relativePathColumn = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val path = if (relativePathColumn != -1) {
                    cursor.getString(relativePathColumn).orEmpty()
                } else {
                    ""
                }

                val contentUri = android.content.ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                videos.add(
                    DeviceVideo(
                        id = id,
                        name = name,
                        duration = duration,
                        size = size,
                        path = path,
                        uri = contentUri
                    )
                )
            }
        }

        videos
    }
}

/**
 * Represents a video found on device
 */
data class DeviceVideo(
    val id: Long,
    val name: String,
    val duration: Long,
    val size: Long,
    val path: String,
    val uri: Uri
) {
    fun getFormattedDuration(): String {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun getFormattedSize(): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
}
