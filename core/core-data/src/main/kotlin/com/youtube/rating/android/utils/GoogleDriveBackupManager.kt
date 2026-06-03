package com.youtube.rating.android.utils

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import android.util.Base64
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.storage.FavoriteVideo
import com.youtube.rating.android.storage.FavoritesManager
import com.youtube.rating.android.storage.FastingManager
import com.youtube.rating.android.storage.RosaryManager
import com.youtube.rating.android.storage.TaskManager
import com.youtube.rating.shared.backup.BackupKeys
import com.youtube.rating.shared.data.NotesRepository
import com.youtube.rating.shared.data.OfflineVideosRepository
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.youtube.rating.shared.bytearray.buildByteArray
import com.youtube.rating.android.data.prefs.AdminPrefs
import com.youtube.rating.android.data.prefs.BrightnessPrefs
import com.youtube.rating.android.data.prefs.GalleryPrefs
import com.youtube.rating.android.data.prefs.HomePrefs
import com.youtube.rating.android.data.prefs.InstallPrefs

/**
 * Google Drive backup manager (context = via SAF file picker)
 *
 * ✅ Backup/restore:
 * - favorites
 * - rosarySessions
 * - notes
 * - offlineVideos metadata
 * - offline thumbnails (embedded Base64)
 * - ✅ NEW: Gallery images (embedded Base64) + restore to filesDir
 * - ✅ NEW: Fasting entries + settings
 */
class GoogleDriveBackupManager(
    context: Context,
    private val favoritesManager: FavoritesManager,
    private val rosaryManager: RosaryManager,
    private val notesRepository: NotesRepository,
    private val offlineVideosRepository: OfflineVideosRepository,
    private val taskManager: TaskManager
) {
    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "GoogleDriveBackup"

        // Limits to keep JSON from exploding too much
        private const val MAX_THUMB_BYTES = 2 * 1024 * 1024   // 2MB per thumbnail
        private const val MAX_GALLERY_IMAGE_BYTES = 6 * 1024 * 1024 // 6MB per gallery image
        private const val MAX_TOTAL_GALLERY_BYTES = 40 * 1024 * 1024 // 40MB total embedded

        // Gallery folder candidates (best-effort)
        private val GALLERY_DIR_CANDIDATES = listOf(
            "gallery_images",
            "gallery",
            "images",
            "photo_gallery",
            "offline_images",
            "pictures"
        )

        // Deprecated singleton removed (it leaked Context via static `instance`).
    }

    data class BackupData(
        val timestamp: Long,
        val appVersion: String,
        val favorites: List<FavoriteVideo>,
        val rosarySessions: JSONArray,
        val notes: JSONArray,
        val offlineVideos: JSONArray,
        val tasks: JSONArray,
        val galleryImages: JSONArray,
        val preferences: JSONObject,
        val fastingEntries: JSONArray,
        val fastingWeeklyGoal: Int,
        val fastingReminder: JSONObject
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put(BackupKeys.TIMESTAMP, timestamp)
                put(BackupKeys.APP_VERSION, appVersion)
                put(BackupKeys.EXPORT_DATE, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))

                val favoritesArray = JSONArray()
                favorites.forEach { fav -> favoritesArray.put(fav.toJson()) }
                put(BackupKeys.FAVORITES, favoritesArray)

                put(BackupKeys.ROSARY_SESSIONS, rosarySessions)
                put(BackupKeys.NOTES, notes)
                put(BackupKeys.OFFLINE_VIDEOS, offlineVideos)
                put(BackupKeys.TASKS, tasks)

                put(BackupKeys.GALLERY_IMAGES, galleryImages)
                put(BackupKeys.PREFERENCES, preferences)
                put(BackupKeys.FASTING_ENTRIES, fastingEntries)
                put(BackupKeys.FASTING_WEEKLY_GOAL, fastingWeeklyGoal)
                put(BackupKeys.FASTING_REMINDER, fastingReminder)
            }
        }

        companion object {
            fun fromJson(json: JSONObject): BackupData {
                val favoritesArray = json.getJSONArray(BackupKeys.FAVORITES)
                val favorites = mutableListOf<FavoriteVideo>()
                for (i in 0 until favoritesArray.length()) {
                    favorites.add(FavoriteVideo.fromJson(favoritesArray.getJSONObject(i)))
                }

                val rosaryArray = json.getJSONArray(BackupKeys.ROSARY_SESSIONS)
                val notesArray = json.optJSONArray(BackupKeys.NOTES) ?: JSONArray()
                val offlineArray = json.optJSONArray(BackupKeys.OFFLINE_VIDEOS) ?: JSONArray()
                val tasksArray = json.optJSONArray(BackupKeys.TASKS) ?: JSONArray()

                val galleryArray = json.optJSONArray(BackupKeys.GALLERY_IMAGES) ?: JSONArray()
                val prefsJson = json.optJSONObject(BackupKeys.PREFERENCES) ?: JSONObject()
                val fastingEntries = json.optJSONArray(BackupKeys.FASTING_ENTRIES) ?: JSONArray()
                val fastingWeeklyGoal = json.optInt(BackupKeys.FASTING_WEEKLY_GOAL, 3)
                val fastingReminder = json.optJSONObject(BackupKeys.FASTING_REMINDER) ?: JSONObject()

                return BackupData(
                    timestamp = json.getLong(BackupKeys.TIMESTAMP),
                    appVersion = json.getString(BackupKeys.APP_VERSION),
                    favorites = favorites,
                    rosarySessions = rosaryArray,
                    notes = notesArray,
                    offlineVideos = offlineArray,
                    tasks = tasksArray,
                    galleryImages = galleryArray,
                    preferences = prefsJson,
                    fastingEntries = fastingEntries,
                    fastingWeeklyGoal = fastingWeeklyGoal,
                    fastingReminder = fastingReminder
                )
            }
        }
    }

    // =========================
    // Base64 file helpers
    // =========================

    private fun isImageFile(file: File): Boolean {
        val ext = file.extension.lowercase(Locale.ROOT)
        return ext in setOf("jpg", "jpeg", "png", "webp", "gif")
    }

    private fun encodeFileToBase64(path: String?, maxBytes: Int): Pair<String, String>? {
        if (path.isNullOrBlank()) return null
        return try {
            val f = File(path)
            if (!f.exists() || !f.isFile) return null
            val len = f.length()
            if (len <= 0L || len > maxBytes.toLong()) return null

            val ext = f.extension.ifBlank { "jpg" }
            val b64 = Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
            b64 to ext
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }

    private fun encodeFileToBase64(file: File, maxBytes: Int): Pair<String, String>? {
        return try {
            if (!file.exists() || !file.isFile) return null
            val len = file.length()
            if (len <= 0L || len > maxBytes.toLong()) return null

            val ext = file.extension.ifBlank { "jpg" }
            val b64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            b64 to ext
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }

    private fun restoreBase64ToFile(dir: File, fileName: String, base64: String): String? {
        return try {
            dir.mkdirs()
            val out = File(dir, fileName)
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            // Use ByteArrayBuilder helper for composable binary payload building.
            val payload = buildByteArray {
                append(bytes)
            }
            out.writeBytes(payload)
            out.absolutePath
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }

    private fun getOfflineThumbDir(): File {
        return File(appContext.filesDir, "offline_thumbnails").apply { mkdirs() }
    }

    private fun restoreBase64ToThumbnailFile(id: String, base64: String, ext: String): String? {
        return try {
            val dir = getOfflineThumbDir()
            val safeExt = ext.ifBlank { "jpg" }
            val out = File(dir, "thumb_${id}.$safeExt")
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            out.writeBytes(bytes)
            out.absolutePath
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }

    // =========================
    // Gallery directory helpers
    // =========================

    /**
     * Best-effort:
     * - ako postoji neki od poznatih foldera u filesDir, uzmi njih
     * - ako nijedan ne postoji, koristi filesDir/gallery_images
     */
    private fun getGalleryDirsForBackup(): List<File> {
        val existing = GALLERY_DIR_CANDIDATES
            .map { File(appContext.filesDir, it) }
            .filter { it.exists() && it.isDirectory }

        return if (existing.isNotEmpty()) existing else listOf(File(appContext.filesDir, "gallery_images"))
    }

    /**
     * Gdje ćemo restore-ati galeriju:
     * - ako postoji neki od kandidata, u prvi postojeći
     * - inače kreiraj gallery_images
     */
    private fun getGalleryRestoreDir(): File {
        val existing = GALLERY_DIR_CANDIDATES
            .map { File(appContext.filesDir, it) }
            .firstOrNull { it.exists() && it.isDirectory }

        return existing ?: File(appContext.filesDir, "gallery_images").apply { mkdirs() }
    }

    // =========================
    // Create backup data
    // =========================

    suspend fun createBackupData(): BackupData = withContext(ioDispatcher) {
        try {
            val favorites = favoritesManager.getFavoritesSuspend()
            val rosarySessions = rosaryManager.exportSessions()

            // Notes
            var notesArray = JSONArray()
            try {
                val notesRepo = notesRepository
                notesArray = BackupJsonCodec.notesToJsonArray(notesRepo.getAllNotes().first())
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "Error exporting notes", e)
            }

            // Offline videos (+ thumbnail embed)
            val offlineArray = JSONArray()
            try {
                val videosRepo = offlineVideosRepository
                videosRepo.getAllVideos().first().forEach { video ->
                    val thumbPath = video.thumbnailPath?.takeIf { it.isNotBlank() }
                    val encodedThumb = encodeFileToBase64(thumbPath, MAX_THUMB_BYTES)

                    offlineArray.put(
                        JSONObject().apply {
                            put("id", video.id)
                            put("youtubeId", video.youtubeId ?: "")
                            put("title", video.title)
                            put("channelName", video.channelName)
                            put("localPath", video.localPath)

                            put("thumbnailPath", thumbPath ?: "")
                            put("thumbnailUrl", video.thumbnailUrl ?: "")

                            put("thumbnailBase64", encodedThumb?.first ?: "")
                            put("thumbnailExt", encodedThumb?.second ?: "")

                            put("duration", video.duration)
                            put("fileSize", video.fileSize)
                            put("category", video.category ?: "")
                            put("addedAt", video.addedAt)
                        }
                    )
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "Error exporting offline videos", e)
            }

            // ✅ NEW: Gallery images (embed Base64)
            val galleryArray = JSONArray()
            var totalEmbedded = 0L

            try {
                val dirs = getGalleryDirsForBackup()
                dirs.forEach { dir ->
                    dir.mkdirs()
                    val files = dir.listFiles()?.toList().orEmpty()
                        .filter { it.isFile && isImageFile(file = it) }
                        .sortedByDescending { it.lastModified() }

                    files.forEach { file ->
                        if (totalEmbedded >= MAX_TOTAL_GALLERY_BYTES) return@forEach

                        val encoded = encodeFileToBase64(file, MAX_GALLERY_IMAGE_BYTES) ?: return@forEach
                        val b64 = encoded.first
                        val ext = encoded.second

                        // rough estimate: base64 ~ 4/3 size
                        val approxBytes = (file.length() * 4L) / 3L
                        if (totalEmbedded + approxBytes > MAX_TOTAL_GALLERY_BYTES) return@forEach

                        totalEmbedded += approxBytes

                        galleryArray.put(
                            JSONObject().apply {
                                // Keep original filename for easier reuse in UI
                                put("fileName", file.name)
                                put("ext", ext)
                                put("lastModified", file.lastModified())
                                put("size", file.length())

                                // (Optional) store sourceDir name
                                put("sourceDir", dir.name)

                                // The image data
                                put("base64", b64)
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "Error exporting gallery images", e)
            }

            val prefsJson = JSONObject().apply {
                put("gridView", HomePrefs.isGridView(appContext))
                put("appBrightness", BrightnessPrefs.getAppBrightness(appContext, 1.0f))
                put("adminMode", AdminPrefs.getAdminMode(appContext))
                put(BackupKeys.GALLERY_IMAGES, JSONArray(GalleryPrefs.getGalleryImagesJson(appContext)))
                put(BackupKeys.GALLERY_PINNED, JSONArray(GalleryPrefs.getGalleryPinnedJson(appContext)))
                put("installId", InstallPrefs.getInstallId(appContext))

                val lm = com.youtube.rating.android.localization.LanguageManager(appContext)
                put(BackupKeys.APP_LANGUAGE, lm.loadLanguage().name)

                val clm = com.youtube.rating.android.localization.ContentLanguageManager(appContext)
                put(BackupKeys.CONTENT_LANGUAGES, JSONArray(clm.getSelectedContentLanguages().map { it.name }))
            }
            val tasksArray = taskManager.exportTasks()
            val fastingManager = FastingManager.getInstance(appContext)
            val fastingEntries = fastingManager.exportEntries()
            val fastingWeeklyGoal = fastingManager.getWeeklyGoal()
            val fastingReminder = BackupJsonCodec.fastingReminderToJson(fastingManager.getReminderSettings())

            Logger.info(
                TAG,
                "Backup created: ${favorites.size} favorites, ${rosarySessions.length()} rosary sessions, " +
                        "${notesArray.length()} notes, ${offlineArray.length()} offline videos, " +
                        "${tasksArray.length()} tasks, ${fastingEntries.length()} fasting entries, " +
                        "${galleryArray.length()} gallery images (embedded approx ${totalEmbedded / (1024 * 1024)}MB)"
            )

            BackupData(
                timestamp = System.currentTimeMillis(),
                appVersion = BuildConfig.VERSION_NAME,
                favorites = favorites,
                rosarySessions = rosarySessions,
                notes = notesArray,
                offlineVideos = offlineArray,
                tasks = tasksArray,
                galleryImages = galleryArray,
                preferences = prefsJson,
                fastingEntries = fastingEntries,
                fastingWeeklyGoal = fastingWeeklyGoal,
                fastingReminder = fastingReminder
            )
        } catch (e: Exception) {
            Logger.error(TAG, "Error creating backup data", e)
            com.youtube.rating.android.sentry.SentryLogger.captureException(
                e,
                tags = mapOf("where" to "GoogleDriveBackupManager.createBackupData")
            )
            // Fallback: return an empty backup instead of crashing export flows.
            BackupData(
                timestamp = System.currentTimeMillis(),
                appVersion = BuildConfig.VERSION_NAME,
                favorites = emptyList(),
                rosarySessions = JSONArray(),
                notes = JSONArray(),
                offlineVideos = JSONArray(),
                tasks = JSONArray(),
                galleryImages = JSONArray(),
                preferences = JSONObject(),
                fastingEntries = JSONArray(),
                fastingWeeklyGoal = 0,
                fastingReminder = JSONObject()
            )
        }
    }

    suspend fun exportBackupToString(): String = withContext(ioDispatcher) {
        val backupData = createBackupData()
        backupData.toJson().toString(2)
    }

    // =========================
    // Restore
    // =========================

    suspend fun restoreFromString(jsonString: String): RestoreResult = withContext(ioDispatcher) {
        try {
            val json = JSONObject(jsonString)
            val backupData = BackupData.fromJson(json)

            // Restore notes
            val notesRepo = notesRepository
            var notesRestored = 0
            try {
                notesRepo.clearAll()
                BackupJsonCodec.notesFromJsonArray(backupData.notes).forEach { note ->
                    notesRepo.importNote(note)
                    notesRestored++
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "Error restoring notes", e)
            }

            // Restore offline videos (+ thumbnails)
            val videosRepo = offlineVideosRepository
            var videosRestored = 0
            try {
                videosRepo.clearAll()
                for (i in 0 until backupData.offlineVideos.length()) {
                    val v = backupData.offlineVideos.getJSONObject(i)
                    val id = v.getString("id")

                    val thumbBase64 = v.optString("thumbnailBase64")
                    val thumbExt = v.optString("thumbnailExt")

                    val restoredThumbPath: String? =
                        if (thumbBase64.isNotBlank()) {
                            restoreBase64ToThumbnailFile(id = id, base64 = thumbBase64, ext = thumbExt)
                        } else {
                            v.optString("thumbnailPath")
                                .takeIf { it.isNotBlank() }
                                ?.takeIf { File(it).exists() }
                        }

                    val model = com.youtube.rating.shared.data.OfflineVideoModel(
                        id = id,
                        youtubeId = v.optString("youtubeId").takeIf { it.isNotEmpty() },
                        title = v.getString("title"),
                        channelName = v.optString("channelName", "Unknown"),
                        localPath = v.getString("localPath"),
                        thumbnailPath = restoredThumbPath,
                        thumbnailUrl = v.optString("thumbnailUrl").takeIf { it.isNotEmpty() },
                        duration = v.optLong("duration", 0L),
                        fileSize = v.optLong("fileSize", 0L),
                        category = v.optString("category").takeIf { it.isNotEmpty() },
                        addedAt = v.optLong("addedAt", System.currentTimeMillis())
                    )

                    videosRepo.addVideo(model)
                    videosRestored++
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "Error restoring offline videos", e)
            }

            // ✅ NEW: Restore Gallery images (write files locally)
            var galleryRestored = 0
            try {
                val restoreDir = getGalleryRestoreDir()
                restoreDir.mkdirs()

                // Clear existing gallery before restore (replace mode)
                // To merge instead of replace, comment out this block
                restoreDir.listFiles()
                    ?.filter { it.isFile && isImageFile(file = it) }
                    ?.forEach { file ->
                        if (!file.delete()) {
                            Logger.error(TAG, "Failed to delete gallery image during restore: ${file.absolutePath}")
                        }
                    }

                for (i in 0 until backupData.galleryImages.length()) {
                    val g = backupData.galleryImages.getJSONObject(i)
                    val fileName = g.optString("fileName").ifBlank { "img_${System.currentTimeMillis()}_$i.jpg" }
                    val base64 = g.optString("base64")
                    if (base64.isBlank()) continue

                    val writtenPath = restoreBase64ToFile(dir = restoreDir, fileName = fileName, base64 = base64)
                    if (writtenPath != null) {
                        galleryRestored++
                    }
                }

                Logger.info(TAG, "Gallery restored: $galleryRestored images into ${restoreDir.absolutePath}")
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "Error restoring gallery images", e)
            }

            // Restore preferences/settings
            val prefsJson = backupData.preferences
            if (prefsJson.length() > 0) {
                if (prefsJson.has("gridView")) {
                    HomePrefs.setGridView(
                        appContext,
                        prefsJson.optBoolean("gridView", true)
                    )
                }

                if (prefsJson.has("appBrightness")) {
                    BrightnessPrefs.setAppBrightness(
                        appContext,
                        prefsJson.optDouble("appBrightness", 1.0).toFloat()
                    )
                }

                // Skip admin mode restore for safety.

                if (prefsJson.has(BackupKeys.GALLERY_IMAGES)) {
                    GalleryPrefs.setGalleryImagesJson(
                        appContext,
                        prefsJson.optJSONArray(BackupKeys.GALLERY_IMAGES)?.toString() ?: "[]"
                    )
                }
                if (prefsJson.has(BackupKeys.GALLERY_PINNED)) {
                    GalleryPrefs.setGalleryPinnedJson(
                        appContext,
                        prefsJson.optJSONArray(BackupKeys.GALLERY_PINNED)?.toString() ?: "[]"
                    )
                }

                // Do not restore device/install IDs to avoid identity collisions

                val appLang = prefsJson.optString(BackupKeys.APP_LANGUAGE, "")
                if (appLang.isNotBlank()) {
                    val lm = com.youtube.rating.android.localization.LanguageManager(appContext)
                    try {
                        lm.saveLanguage(com.youtube.rating.android.localization.Strings.Language.valueOf(appLang))
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    }
                }

                val contentLangsArr = prefsJson.optJSONArray(BackupKeys.CONTENT_LANGUAGES)
                if (contentLangsArr != null) {
                    val clm = com.youtube.rating.android.localization.ContentLanguageManager(appContext)
                    val langs = mutableSetOf<com.youtube.rating.android.localization.Strings.Language>()
                    for (i in 0 until contentLangsArr.length()) {
                        try {
                            langs.add(com.youtube.rating.android.localization.Strings.Language.valueOf(contentLangsArr.getString(i)))
                        } catch (e: Exception) {
                            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        }
                    }
                    if (langs.isNotEmpty()) clm.saveContentLanguages(langs)
                }
            }

            // Restore favorites
            favoritesManager.clearAllSuspend()

            var favoritesRestored = 0
            backupData.favorites.forEach { favorite ->
                try {
                    if (favoritesManager.addFavoriteSuspend(favorite)) favoritesRestored++
                } catch (e: Exception) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    Logger.error(TAG, "Error restoring favorite: ${favorite.videoId}", e)
                }
            }

            // Restore rosary sessions
            rosaryManager.importSessions(backupData.rosarySessions)
            val rosaryRestored = backupData.rosarySessions.length()

            // Restore tasks
            val tasksRestored = taskManager.importTasks(backupData.tasks)

            // Restore fasting
            val fastingManager = FastingManager.getInstance(appContext)
            val fastingRestored = fastingManager.importEntries(backupData.fastingEntries)
            if (backupData.fastingWeeklyGoal > 0) {
                fastingManager.setWeeklyGoal(backupData.fastingWeeklyGoal)
            }
            val reminderJson = backupData.fastingReminder
            if (reminderJson.length() > 0) {
                fastingManager.saveReminderSettings(BackupJsonCodec.fastingReminderFromJson(reminderJson))
            }

            Logger.info(
                TAG,
                "Restore completed: $favoritesRestored favorites, $rosaryRestored rosary sessions, " +
                        "$notesRestored notes, $videosRestored offline videos, $tasksRestored tasks, " +
                        "$fastingRestored fasting entries, " +
                        "$galleryRestored gallery images"
            )

            // RestoreStats nema gallery field - ne diramo postojeći model
            RestoreResult.Success(
                RestoreStats(
                    notesRestored = notesRestored,
                    favoritesRestored = favoritesRestored,
                    offlineVideosRestored = videosRestored,
                    rosarySessionsRestored = rosaryRestored,
                    tasksRestored = tasksRestored,
                    fastingRestored = fastingRestored
                )
            )
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Error restoring from backup", e)
            RestoreResult.Failed(e.message ?: "Unknown error")
        }
    }

    fun getSuggestedFilename(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "youtube_rating_backup_$timestamp.json"
    }
}
