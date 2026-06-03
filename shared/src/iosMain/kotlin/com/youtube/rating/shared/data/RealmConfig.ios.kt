@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.youtube.rating.shared.data

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

/**
 * iOS-specific Realm encryption key management using Keychain
 */
actual fun getEncryptionKeyPlatform(): ByteArray {
    val keyAlias = "realm_encryption_key"

    // Try to retrieve existing key from Keychain
    val existingKey = retrieveKeyFromKeychain(keyAlias = keyAlias)
    if (existingKey != null) {
        return deriveKeyFromData(keyData = existingKey)
    }

    // Generate new random key and store it in Keychain
    val newKeyData = generateRandomKeyData()
    storeKeyInKeychain(keyAlias = keyAlias, keyData = newKeyData)

    return deriveKeyFromData(keyData = newKeyData)
}

/**
 * Retrieve key data from iOS Keychain
 */
private fun retrieveKeyFromKeychain(keyAlias: String): NSData? {
    val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 5, null, null)

    CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
    CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(keyAlias) as CFStringRef?)
    CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain("com.youtube.rating.realm") as CFStringRef?)
    CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
    CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

    memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)

        if (status == errSecSuccess) {
            return CFBridgingRelease(result.value) as? NSData
        }
    }

    return null
}

/**
 * Store key data in iOS Keychain
 */
private fun storeKeyInKeychain(keyAlias: String, keyData: NSData) {
    val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 5, null, null)

    CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
    CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(keyAlias) as CFStringRef?)
    CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain("com.youtube.rating.realm") as CFStringRef?)
    CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(keyData) as CFDataRef?)

    SecItemAdd(query, null)
}

/**
 * Generate random 32-byte key data
 */
private fun generateRandomKeyData(): NSData {
    val keyLength = 32
    val keyBytes = ByteArray(keyLength)

    keyBytes.usePinned { pinned ->
        val status = SecRandomCopyBytes(kSecRandomDefault, keyLength.convert(), pinned.addressOf(0))
        if (status != errSecSuccess) {
            // Fallback: fill with zeros if secure random fails
            for (i in 0 until keyLength) {
                keyBytes[i] = 0
            }
        }
    }

    return keyBytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), keyLength.convert())
    }
}

/**
 * Derive a 64-byte key from the stored key data for Realm encryption
 * Realm requires exactly 64 bytes for encryption
 */
private fun deriveKeyFromData(keyData: NSData): ByteArray {
    val keyBytes = ByteArray(keyData.length.toInt())
    keyBytes.usePinned { pinned ->
        keyData.getBytes(pinned.addressOf(0), keyData.length)
    }

    val result = ByteArray(64)

    // Use the key bytes to fill the 64-byte array
    // If key is shorter than 64 bytes, repeat it
    for (i in result.indices) {
        result[i] = keyBytes[i % keyBytes.size]
    }

    return result
}
