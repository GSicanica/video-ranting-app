package com.youtube.rating.shared.data

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.migration.AutomaticSchemaMigration

/**
 * Realm configuration for the app - KMP style
 */
object RealmProvider {
    
    private val initLock = Any()
    private val realmThreadLocal = ThreadLocal<Realm?>()
    
    /**
     * Generate or retrieve encryption key for Realm database
     * Uses platform-specific secure storage (KeyStore on Android, Keychain on iOS)
     */
    private fun getEncryptionKey(): ByteArray {
        return getEncryptionKeyPlatform()
    }
    
    /**
     * Initialize Realm with schema
     */
    fun initialize(): Realm {
        synchronized(initLock) {
            val existing = realmThreadLocal.get()
            if (existing != null) return existing
            if (realmThreadLocal.get() == null) {
                realmThreadLocal.set(openEncryptedOrMigrate())
            }
            return realmThreadLocal.get() ?: throw IllegalStateException("Realm initialization failed")
        }
    }

    private fun openEncryptedOrMigrate(): Realm {
        val config = createEncryptedConfig()
        return try {
            Realm.open(config)
        } catch (encryptedOpenError: Exception) {
            println("Encrypted Realm failed, attempting migration from unencrypted database: ${encryptedOpenError.message}")
            ensureNotMainThread(operation = "migrateFromUnencryptedDatabase")
            runCatching {
                migrateFromUnencryptedDatabase()
                Realm.open(config)
            }.getOrElse { migrationError ->
                throw IllegalStateException(
                    "Realm could not be opened and migration failed. Existing Realm files were preserved.",
                    migrationError
                )
            }
        }
    }

    private fun ensureNotMainThread(operation: String) {
        if (isMainThread()) {
            throw IllegalStateException("Realm initialization requires IO for $operation. Call from a background thread.")
        }
    }
    
    /**
     * Create encrypted Realm configuration
     */
    private fun createEncryptedConfig(): RealmConfiguration {
        return RealmConfiguration.Builder(
            schema = setOf(
                NoteEntity::class,
                FavoriteVideoEntity::class,
                OfflineVideoEntity::class,
                AppDataEntity::class,
                HomeScreenCacheEntity::class
            )
        )
        .name("youtube_ratings.realm")
        .schemaVersion(7) // Incremented version after removing PlaylistEntity
        .encryptionKey(getEncryptionKey()) // Enable database encryption
        .compactOnLaunch { totalBytes, usedBytes ->
            // Compact database if more than 50% is empty and file is larger than 50MB
            val ratio = usedBytes.toDouble() / totalBytes
            val shouldCompact = ratio < 0.5 && totalBytes > 50 * 1024 * 1024
            shouldCompact
        }
        // Automatic schema migration keeps existing data when schema changes.
        .migration(AutomaticSchemaMigration { /* no-op: rely on automatic schema migration */ })
        .build()
    }
    
    /**
     * Migrate data from unencrypted database to encrypted one
     */
    private fun migrateFromUnencryptedDatabase() {
        try {
            // Open unencrypted database to read existing data
            val unencryptedConfig = RealmConfiguration.Builder(
                schema = setOf(
                    NoteEntity::class,
                    FavoriteVideoEntity::class,
                    OfflineVideoEntity::class,
                    AppDataEntity::class,
                    HomeScreenCacheEntity::class
                )
            )
            .name("youtube_ratings.realm")
            .schemaVersion(7)
            .build()
            
            val unencryptedRealm = Realm.open(unencryptedConfig)
            
            // Extract data from unencrypted database
            val notes = unencryptedRealm.query(NoteEntity::class).find().map { note ->
                NoteEntity().apply {
                    id = note.id
                    title = note.title
                    content = note.content
                    timestamp = note.timestamp
                    lastModified = note.lastModified
                }
            }
            val favorites = unencryptedRealm.query(FavoriteVideoEntity::class).find().map { favorite ->
                FavoriteVideoEntity().apply {
                    videoId = favorite.videoId
                    title = favorite.title
                    thumbnail = favorite.thumbnail
                    channelName = favorite.channelName
                    avgLove = favorite.avgLove
                    avgFaith = favorite.avgFaith
                    avgHope = favorite.avgHope
                    totalRatings = favorite.totalRatings
                    category = favorite.category
                    timestamp = favorite.timestamp
                    type = favorite.type
                }
            }
            val offlineVideos = unencryptedRealm.query(OfflineVideoEntity::class).find().map { offline ->
                OfflineVideoEntity().apply {
                    id = offline.id
                    youtubeId = offline.youtubeId
                    title = offline.title
                    channelName = offline.channelName
                    localPath = offline.localPath
                    thumbnailPath = offline.thumbnailPath
                    thumbnailUrl = offline.thumbnailUrl
                    duration = offline.duration
                    fileSize = offline.fileSize
                    category = offline.category
                    addedAt = offline.addedAt
                }
            }
            
            // Close unencrypted database
            unencryptedRealm.close()
            
            // Delete the unencrypted file only after the data snapshot above succeeded.
            deleteUnencryptedRealmAfterSuccessfulSnapshot()
            
            // Create encrypted database and populate with migrated data
            val encryptedConfig = createEncryptedConfig()
            val encryptedRealm = Realm.open(encryptedConfig)
            
            encryptedRealm.writeBlocking {
                // Copy notes
                notes.forEach { note -> copyToRealm(note) }
                
                // Copy favorites
                favorites.forEach { favorite -> copyToRealm(favorite) }
                
                // Copy offline videos
                offlineVideos.forEach { offline -> copyToRealm(offline) }
            }
            
            encryptedRealm.close()
            println("Successfully migrated data from unencrypted to encrypted database")
            
        } catch (e: Exception) {
            println("Failed to migrate from unencrypted database: ${e.message}")
            throw e
        }
    }

    private fun deleteUnencryptedRealmAfterSuccessfulSnapshot() {
        try {
            val config = RealmConfiguration.Builder(
                schema = setOf(
                    NoteEntity::class,
                    FavoriteVideoEntity::class,
                    OfflineVideoEntity::class,
                    AppDataEntity::class,
                    HomeScreenCacheEntity::class
                )
            )
            .name("youtube_ratings.realm")
            .schemaVersion(7)
            .build()

            Realm.deleteRealm(config)
            println("Successfully deleted unencrypted Realm database files after snapshot")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to replace unencrypted Realm database", e)
        }
    }
    
    /**
     * Get existing Realm instance or create new one
     */
    fun getInstance(): Realm {
        return realmThreadLocal.get() ?: initialize()
    }
    
    /**
     * Close Realm instance
     */
    fun close() {
        realmThreadLocal.get()?.close()
        realmThreadLocal.remove()
    }
}

/**
 * Factory for creating repositories - KMP style
 */
object RepositoryFactory {
    
    fun createNotesRepository(): NotesRepository {
        return NotesRepositoryImpl(realm = RealmProvider.getInstance())
    }
    
    fun createFavoritesRepository(): FavoritesRepository {
        return FavoritesRepositoryImpl(RealmProvider.getInstance())
    }
    
    fun createOfflineVideosRepository(): OfflineVideosRepository {
        return OfflineVideosRepositoryImpl(realm = RealmProvider.getInstance())
    }
    
    fun createHomeScreenCacheRepository(): HomeScreenCacheRepository {
        return HomeScreenCacheRepositoryImpl(realm = RealmProvider.getInstance())
    }
}

// Platform-specific expect declarations moved to commonMain (PlatformRealm.kt).
