package com.youtube.rating.android.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import kotlin.time.Duration.Companion.seconds
import com.youtube.rating.android.data.prefs.GalleryPrefs
import com.youtube.rating.core.coroutines.makeIOCall

private const val TAG = "GalleryViewModel"

class GalleryViewModel(
    private val apiClient: RatingApiClient
) : ViewModel() {

    private val _galleryImages = MutableStateFlow<List<String>>(emptyList())
    val galleryImages: StateFlow<List<String>> = _galleryImages.asStateFlow()

    private val _serverImages = MutableStateFlow<List<String>>(emptyList())
    val serverImages: StateFlow<List<String>> = _serverImages.asStateFlow()

    private val _isSyncingServer = MutableStateFlow(false)
    val isSyncingServer: StateFlow<Boolean> = _isSyncingServer.asStateFlow()

    private val _lastServerSyncAt = MutableStateFlow<Long?>(null)
    val lastServerSyncAt: StateFlow<Long?> = _lastServerSyncAt.asStateFlow()

    private val _pinnedImages = MutableStateFlow<List<String>>(emptyList())
    val pinnedImages: StateFlow<List<String>> = _pinnedImages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    private fun setError(message: String, e: Exception? = null, logMessage: String = message) {
        if (e != null) com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        if (e != null) Logger.error(TAG, logMessage, e) else Logger.error(TAG, logMessage)
        _error.value = message
    }

    private suspend fun recomputeUiState(context: Context, localImages: List<String>? = null) {
        val local = localImages ?: loadGalleryImagesFromStorage(context = context)
        val pinned = loadPinnedFromStorage(context = context)
        _pinnedImages.value = pinned

        val allImages = mergeUnique(primary = local, secondary = _serverImages.value)
        _galleryImages.value = sortWithPinned(images = allImages, pinned = pinned)
    }

    fun loadImages(context: Context) {
        makeIOCall {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ Prvo učitaj default slike iz assets ako još nisu učitane
                loadDefaultImagesIfNeeded(context = context)

                val localImages = loadGalleryImagesFromStorage(context = context)
                val server = fetchServerImages()

                _serverImages.value = server
                if (server.isNotEmpty()) _lastServerSyncAt.value = System.currentTimeMillis()

                recomputeUiState(context, localImages = localImages)
            } catch (e: Exception) {
                setError(message = e.message ?: "Failed to load images", e = e, logMessage = "Error loading images")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshServer(context: Context) {
        makeIOCall {
            if (_isSyncingServer.value) return@makeIOCall
            _isSyncingServer.value = true
            try {
                // short delay to prevent accidental double-taps showing no feedback
                delay(0.2.seconds)

                val server = fetchServerImages()
                _serverImages.value = server
                _lastServerSyncAt.value = System.currentTimeMillis()

                recomputeUiState(context)
            } catch (e: Exception) {
                setError(message = e.message ?: "Server refresh failed", e = e, logMessage = "Error refreshing server images")
            } finally {
                _isSyncingServer.value = false
            }
        }
    }

    /**
     * ✅ Multi add: spremi sve, update state samo jednom (brže i “čišće”)
     */
    fun addImagesFromDevice(
        context: Context,
        uris: List<Uri>,
        onComplete: (added: Int, failed: Int) -> Unit
    ) {
        makeIOCall {
            _isLoading.value = true
            var added = 0
            var failed = 0

            try {
                val images = loadGalleryImagesFromStorage(context = context).toMutableList()

                for (uri in uris) {
                    val path = saveImageToStorage(context = context, uri = uri)
                    if (path != null) {
                        if (images.contains(path)) {
                            failed++
                        } else {
                            images.add(path)
                            added++
                        }
                    } else {
                        failed++
                    }
                }

                if (added > 0) {
                    saveGalleryImagesToStorage(context = context, images = images)

                    recomputeUiState(context, localImages = images)
                }

                withContext(Dispatchers.Main) { onComplete(added, failed) }
            } catch (e: Exception) {
                setError(message = e.message ?: "Failed to add images", e = e, logMessage = "Error adding images")
                withContext(Dispatchers.Main) { onComplete(0, uris.size) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteImage(context: Context, imageUri: String) {
        makeIOCall {
            try {
                if (isRemoteImage(uri = imageUri)) {
                    _error.value = "Server slike se ne mogu obrisati iz aplikacije"
                    return@makeIOCall
                }

                val images = loadGalleryImagesFromStorage(context = context).toMutableList()
                images.remove(imageUri)
                saveGalleryImagesToStorage(context = context, images = images)

                val pinned = loadPinnedFromStorage(context = context).toMutableList()
                pinned.remove(imageUri)
                savePinnedToStorage(context = context, pinned = pinned)

                deleteImageFile(imageUri = imageUri)

                recomputeUiState(context, localImages = images)
            } catch (e: Exception) {
                setError(message = e.message ?: "Failed to delete image", e = e, logMessage = "Error deleting image")
            }
        }
    }

    fun togglePin(context: Context, imageUri: String) {
        makeIOCall {
            try {
                val pinned = loadPinnedFromStorage(context = context).toMutableList()
                if (pinned.contains(imageUri)) pinned.remove(imageUri) else pinned.add(0, imageUri)
                savePinnedToStorage(context = context, pinned = pinned)

                recomputeUiState(context)
            } catch (e: Exception) {
                setError(message = e.message ?: "Failed to toggle pin", e = e, logMessage = "Error togglePin")
            }
        }
    }

    fun saveAnnotatedImage(
        context: Context,
        sourceImageUri: String,
        annotatedBitmap: Bitmap,
        onComplete: (savedPath: String?) -> Unit
    ) {
        makeIOCall {
            _isLoading.value = true
            try {
                if (isRemoteImage(uri = sourceImageUri)) {
                    _error.value = "Server slike se ne mogu uređivati"
                    withContext(Dispatchers.Main) { onComplete(null) }
                    return@makeIOCall
                }

                val savedPath = saveBitmapToGalleryStorage(
                    context = context,
                    sourceImageUri = sourceImageUri,
                    bitmap = annotatedBitmap
                )

                if (savedPath == null) {
                    withContext(Dispatchers.Main) { onComplete(null) }
                    return@makeIOCall
                }

                val images = loadGalleryImagesFromStorage(context = context).toMutableList()
                images.add(0, savedPath)
                saveGalleryImagesToStorage(context = context, images = images)
                recomputeUiState(context, localImages = images)

                withContext(Dispatchers.Main) { onComplete(savedPath) }
            } catch (e: Exception) {
                setError(
                    message = e.message ?: "Failed to save edited image",
                    e = e,
                    logMessage = "Error saving annotated image"
                )
                withContext(Dispatchers.Main) { onComplete(null) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ---------------- HELPERS ----------------

    private fun sortWithPinned(images: List<String>, pinned: List<String>): List<String> {
        val pinnedSet = pinned.toHashSet()
        val pinnedOrdered = pinned.filter { images.contains(it) }
        val rest = images.filterNot { pinnedSet.contains(it) }
        return pinnedOrdered + rest
    }

    private fun mergeUnique(primary: List<String>, secondary: List<String>): List<String> {
        val out = LinkedHashSet<String>(primary.size + secondary.size)
        primary.forEach { out.add(it) }
        secondary.forEach { out.add(it) }
        return out.toList()
    }

    private fun isRemoteImage(uri: String): Boolean {
        return uri.startsWith("http://") || uri.startsWith("https://")
    }

    private suspend fun fetchServerImages(): List<String> {
        return try {
            val res = apiClient.getGalleryList(forceRefresh = true)
            if (!res.success) return emptyList()
            res.images.mapNotNull { it.url.takeIf { u -> u.isNotBlank() } }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Error fetching server gallery images", e)
            emptyList()
        }
    }

    private suspend fun loadDefaultImagesIfNeeded(context: Context) {
        try {
            // Provjeri da li su default slike već učitane
            val hasLoadedDefaults = GalleryPrefs.getGalleryDefaultsLoaded(context)
            if (hasLoadedDefaults) return

            // Učitaj default slike iz assets/images foldera
            val assetManager = context.assets
            val imagesFolder = "images"

            val defaultImages = assetManager.list(imagesFolder)?.mapNotNull { fileName ->
                try {
                    // Kopiraj sliku iz assets u internal storage
                    val inputStream = assetManager.open("$imagesFolder/$fileName")
                    val galleryDir = File(context.filesDir, "gallery")
                    if (!galleryDir.exists()) galleryDir.mkdirs()

                    val imageFile = File(galleryDir, fileName)
                    imageFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()

                    imageFile.absolutePath
                } catch (e: Exception) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    Logger.error(TAG, "Error copying default image $fileName", e)
                    null
                }
            } ?: emptyList()

            if (defaultImages.isNotEmpty()) {
                // Dodaj default slike u postojeću listu
                val existingImages = loadGalleryImagesFromStorage(context = context).toMutableList()
                existingImages.addAll(defaultImages)
                saveGalleryImagesToStorage(context = context, images = existingImages)

                // Označi da su default slike učitane
                GalleryPrefs.setGalleryDefaultsLoaded(context, true)

                Logger.debug(TAG, "Loaded ${defaultImages.size} default images")
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Error loading default images", e)
        }
    }

    private suspend fun saveGalleryImagesToStorage(context: Context, images: List<String>) {
        val arr = JSONArray(images)
        GalleryPrefs.setGalleryImagesJson(context, arr.toString())
    }

    private suspend fun loadGalleryImagesFromStorage(context: Context): List<String> {
        val json = GalleryPrefs.getGalleryImagesJson(context)
        return parseJsonList(json = json)
    }

    private suspend fun loadPinnedFromStorage(context: Context): List<String> {
        val json = GalleryPrefs.getGalleryPinnedJson(context)
        return parseJsonList(json = json)
    }

    private suspend fun savePinnedToStorage(context: Context, pinned: List<String>) {
        val arr = JSONArray(pinned)
        GalleryPrefs.setGalleryPinnedJson(context, arr.toString())
    }

    private fun parseJsonList(json: String): List<String> {
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) add(arr.getString(i))
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "JSON parse error", e)
            emptyList()
        }
    }

    private fun saveImageToStorage(context: Context, uri: Uri): String? {
        return try {
            val galleryDir = File(context.filesDir, "gallery")
            if (!galleryDir.exists()) galleryDir.mkdirs()

            val ext = guessExtension(context = context, uri = uri) ?: "jpg"
            val fileName = "img_${System.currentTimeMillis()}.$ext"
            val imageFile = File(galleryDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            imageFile.absolutePath
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Error saving image", e)
            null
        }
    }

    private fun guessExtension(context: Context, uri: Uri): String? {
        return try {
            val type = context.contentResolver.getType(uri) ?: return null
            MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }

    private fun saveBitmapToGalleryStorage(
        context: Context,
        sourceImageUri: String,
        bitmap: Bitmap
    ): String? {
        return try {
            val galleryDir = File(context.filesDir, "gallery")
            if (!galleryDir.exists()) galleryDir.mkdirs()

            val sourceExt = sourceImageUri.substringAfterLast('.', "jpg").lowercase()
            val ext = if (sourceExt == "png") "png" else "jpg"
            val format = if (ext == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val fileName = "img_edited_${System.currentTimeMillis()}.$ext"
            val outputFile = File(galleryDir, fileName)

            outputFile.outputStream().use { out ->
                val quality = if (format == Bitmap.CompressFormat.JPEG) 92 else 100
                bitmap.compress(format, quality, out)
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Error saving annotated bitmap", e)
            null
        }
    }

    private fun deleteImageFile(imageUri: String) {
        try {
            val file = File(imageUri)
            if (!file.exists()) {
                Logger.error(TAG, "File does not exist: $imageUri")
                return
            }

            // Security: Use canonical path to prevent path traversal attacks
            val canonicalFile = file.canonicalFile
            val canonicalParent = canonicalFile.parentFile?.canonicalFile

            // Verify that the parent directory is actually named "gallery" and is in a safe location
            if (canonicalParent == null || canonicalParent.name != "gallery") {
                Logger.error(TAG, "Security: Attempted to delete file outside gallery directory: $imageUri")
                return
            }

            // Additional check: parent should be a direct child of app's files directory
            val grandParent = canonicalParent.parentFile
            if (grandParent == null || !grandParent.name.contains("files")) {
                Logger.error(TAG, "Security: Gallery parent is not in app's files directory: $imageUri")
                return
            }

            // Attempt deletion and check result
            if (!canonicalFile.delete()) {
                Logger.error(TAG, "Failed to delete file: $imageUri")
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Error deleting file: $imageUri", e)
        }
    }
}
