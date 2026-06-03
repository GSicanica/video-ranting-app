package com.youtube.rating.shared.data

/**
 * Platform-specific hooks used by RealmProvider.
 *
 * Declared in commonMain so iOS can see the expect declarations.
 */
expect fun getEncryptionKeyPlatform(): ByteArray

expect fun isMainThread(): Boolean

