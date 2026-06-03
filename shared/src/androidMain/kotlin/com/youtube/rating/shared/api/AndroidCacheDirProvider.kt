package com.youtube.rating.shared.api

import java.io.File

/**
 * Provides Android cache directory to shared code (KMM-safe).
 * Set once from androidApp Application.onCreate().
 */
object AndroidCacheDirProvider {
    @Volatile
    var cacheDir: File? = null
}