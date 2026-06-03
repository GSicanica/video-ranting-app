package com.youtube.rating.shared.data

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.security.SecureRandom
import kotlin.io.path.exists

/**
 * Desktop-specific Realm encryption key management
 * Stores key in user's home directory
 */
actual fun getEncryptionKeyPlatform(): ByteArray {
    val keyFile = getKeyFile()

    // Check if key file exists
    if (keyFile.exists()) {
        return Files.readAllBytes(keyFile.toPath())
    }

    // Generate new key
    val key = generateRandomKey()
    Files.write(keyFile.toPath(), key)

    return key
}

/**
 * Get the key file path in user's home directory
 */
private fun getKeyFile(): File {
    val userHome = System.getProperty("user.home")
    val appDir = Paths.get(userHome, ".youtube-rating-app")

    // Create directory if it doesn't exist
    if (!appDir.exists()) {
        Files.createDirectories(appDir)
    }

    return appDir.resolve("realm_key.bin").toFile()
}

/**
 * Generate a random 64-byte key for Realm encryption
 */
private fun generateRandomKey(): ByteArray {
    val key = ByteArray(64)
    SecureRandom().nextBytes(key)
    return key
}