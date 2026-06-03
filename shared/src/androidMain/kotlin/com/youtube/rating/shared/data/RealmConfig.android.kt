package com.youtube.rating.shared.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.SecureRandom
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android-specific Realm encryption key management using KeyStore
 */
actual fun getEncryptionKeyPlatform(): ByteArray {
    return AndroidRealmKeyStore.getOrCreateRealmKey()
}

fun initializeAndroidRealmEncryption(context: Context) {
    AndroidRealmKeyStore.initialize(context = context.applicationContext)
}

private object AndroidRealmKeyStore {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "realm_encryption_master_key_v2"
    private const val KEY_FILE_NAME = "realm_key_v2.bin"
    private const val KEY_FILE_VERSION = 1
    private const val GCM_TAG_BITS = 128
    private const val REALM_KEY_BYTES = 64

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getOrCreateRealmKey(): ByteArray {
        val context = appContext
            ?: error("Android Realm encryption context is not initialized")
        val keyFile = File(File(context.filesDir, "realm"), KEY_FILE_NAME)
        keyFile.parentFile?.mkdirs()

        if (keyFile.exists()) {
            return decryptStoredRealmKey(file = keyFile)
        }

        val realmKey = if (legacyRealmFileExists(context = context)) {
            legacyDeterministicRealmKey()
        } else {
            ByteArray(REALM_KEY_BYTES).also { SecureRandom().nextBytes(it) }
        }
        encryptAndStoreRealmKey(file = keyFile, realmKey = realmKey)
        return realmKey
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encryptAndStoreRealmKey(file: File, realmKey: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val ciphertext = cipher.doFinal(realmKey)

        DataOutputStream(file.outputStream()).use { out ->
            out.writeInt(KEY_FILE_VERSION)
            out.writeInt(cipher.iv.size)
            out.write(cipher.iv)
            out.writeInt(ciphertext.size)
            out.write(ciphertext)
        }
    }

    private fun decryptStoredRealmKey(file: File): ByteArray {
        val (iv, ciphertext) = DataInputStream(file.inputStream()).use { input ->
            val version = input.readInt()
            require(version == KEY_FILE_VERSION) { "Unsupported Realm key file version: $version" }
            val ivBytes = ByteArray(input.readInt()).also { input.readFully(it) }
            val cipherBytes = ByteArray(input.readInt()).also { input.readFully(it) }
            ivBytes to cipherBytes
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext).also { key ->
            require(key.size == REALM_KEY_BYTES) { "Invalid Realm key size: ${key.size}" }
        }
    }

    private fun legacyRealmFileExists(context: Context): Boolean {
        return File(context.filesDir, "youtube_ratings.realm").exists()
    }

    private fun legacyDeterministicRealmKey(): ByteArray {
        val seed = "com.youtube.rating.android.realm.encryption.salt.2024".toByteArray(Charsets.UTF_8)
        return ByteArray(REALM_KEY_BYTES) { index ->
            (seed[index % seed.size] + index).toByte()
        }
    }
}
