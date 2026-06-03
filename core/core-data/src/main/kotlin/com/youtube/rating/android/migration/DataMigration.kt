package com.youtube.rating.android.migration

import android.content.Context
import com.youtube.rating.shared.data.FavoriteVideoModel
import com.youtube.rating.shared.data.FavoritesRepository
import com.youtube.rating.shared.data.NoteModel
import com.youtube.rating.shared.data.NotesRepository
import com.youtube.rating.shared.data.OfflineVideoModel
import com.youtube.rating.shared.data.OfflineVideosRepository
import com.youtube.rating.shared.utils.LogConfig
import com.youtube.rating.shared.utils.Logger
import org.json.JSONArray
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named

/**
 * Handles migration of data from old SharedPreferences to new Realm database
 */
object DataMigration {
    
    private const val TAG = "DataMigration"
    private const val MIGRATION_PREFS = "migration_prefs"
    private const val KEY_MIGRATION_COMPLETED = "migration_v1_completed"
    
    // Old SharedPreferences keys
    private const val NOTES_PREFS = "notes_preferences"
    private const val NOTES_KEY = "notes_list"
    
    private const val FAVORITES_PREFS = "favorites_preferences"
    private const val FAVORITES_KEY = "favorites_list"
    
    private const val OFFLINE_PREFS = "offline_videos"
    private const val OFFLINE_KEY = "offline_videos_list"

    private fun sharedNotesRepo(): NotesRepository =
        GlobalContext.get().get(qualifier = named("sharedNotesRepo"))

    private fun sharedFavoritesRepo(): FavoritesRepository =
        GlobalContext.get().get(qualifier = named("sharedFavoritesRepo"))

    private fun sharedOfflineRepo(): OfflineVideosRepository =
        GlobalContext.get().get(qualifier = named("sharedOfflineVideosRepo"))

    private fun log(message: String) {
        if (LogConfig.ENABLE_LOGS) Logger.debug(TAG, message)
    }

    private fun isEmptyJsonList(value: String?): Boolean {
        val v = value?.trim() ?: return true
        return v.isEmpty() || v == "[]"
    }

    private inline fun <T> capture(tag: String, block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, tag, e)
            null
        }
    }
    
    /**
     * Check if migration is needed and perform it
     */
    suspend fun migrateIfNeeded(context: Context): MigrationResult {
        val migrationPrefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        
        // Check if already migrated
        if (migrationPrefs.getBoolean(KEY_MIGRATION_COMPLETED, false)) {
            log("Migration already completed, skipping")
            return MigrationResult.AlreadyMigrated
        }
        
        log(message = "Starting data migration from SharedPreferences to Realm")
        
        return try {
            val stats = MigrationStats()
            
            // Migrate Notes
            val notesCount = migrateNotes(context = context)
            stats.notesMigrated = notesCount
            log(message = "Migrated $notesCount notes")
            
            // Migrate Favorites
            val favoritesCount = migrateFavorites(context = context)
            stats.favoritesMigrated = favoritesCount
            log(message = "Migrated $favoritesCount favorites")
            
            // Migrate Offline Videos
            val videosCount = migrateOfflineVideos(context = context)
            stats.offlineVideosMigrated = videosCount
            log(message = "Migrated $videosCount offline videos")
            
            // Mark migration as completed
            migrationPrefs.edit()
                .putBoolean(KEY_MIGRATION_COMPLETED, true)
                .putLong("migration_timestamp", System.currentTimeMillis())
                .apply()
            
            log(message = "Migration completed successfully: $stats")
            MigrationResult.Success(stats)
            
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Migration failed", e)
            MigrationResult.Failed(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Migrate notes from SharedPreferences to Realm
     */
    private suspend fun migrateNotes(context: Context): Int {
        val prefs = context.getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE)
        val notesJson = prefs.getString(NOTES_KEY, null)
        if (isEmptyJsonList(value = notesJson)) return 0
        
        val repository = sharedNotesRepo()
        var count = 0
        
        capture("Error migrating notes") {
            val jsonArray = JSONArray(notesJson)
            for (i in 0 until jsonArray.length()) {
                val noteJson = jsonArray.getJSONObject(i)
                
                val note = NoteModel(
                    id = noteJson.getString("id"),
                    title = noteJson.getString("title"),
                    content = noteJson.getString("content"),
                    timestamp = noteJson.getLong("timestamp"),
                    lastModified = noteJson.optLong("lastModified", noteJson.getLong("timestamp"))
                )
                
                // Add to Realm
                repository.addNote(note.title, note.content)
                count++
            }
        }
        
        return count
    }
    
    /**
     * Migrate favorites from SharedPreferences to Realm
     */
    private suspend fun migrateFavorites(context: Context): Int {
        val prefs = context.getSharedPreferences(FAVORITES_PREFS, Context.MODE_PRIVATE)
        val favoritesJson = prefs.getString(FAVORITES_KEY, null)
        if (isEmptyJsonList(value = favoritesJson)) return 0
        
        val repository = sharedFavoritesRepo()
        var count = 0
        
        capture("Error migrating favorites") {
            val jsonArray = JSONArray(favoritesJson)
            for (i in 0 until jsonArray.length()) {
                val favoriteJson = jsonArray.getJSONObject(i)
                
                val favorite = FavoriteVideoModel(
                    videoId = favoriteJson.getString("videoId"),
                    title = favoriteJson.getString("title"),
                    thumbnail = favoriteJson.getString("thumbnail"),
                    channelName = favoriteJson.getString("channelName"),
                    avgLove = favoriteJson.getDouble("avgLove"),
                    avgFaith = favoriteJson.getDouble("avgFaith"),
                    avgHope = favoriteJson.getDouble("avgHope"),
                    totalRatings = favoriteJson.getInt("totalRatings"),
                    category = favoriteJson.optString("category").takeIf { it.isNotEmpty() },
                    timestamp = favoriteJson.getLong("timestamp"),
                    type = try {
                        com.youtube.rating.shared.data.FavoriteItemTypeModels.valueOf(favoriteJson.optString("type", "VIDEO"))
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        com.youtube.rating.shared.data.FavoriteItemTypeModels.VIDEO
                    }
                )
                
                repository.addFavorite(favorite)
                count++
            }
        }
        
        return count
    }
    
    /**
     * Migrate offline videos from SharedPreferences to Realm
     */
    private suspend fun migrateOfflineVideos(context: Context): Int {
        val prefs = context.getSharedPreferences(OFFLINE_PREFS, Context.MODE_PRIVATE)
        val videosJson = prefs.getString(OFFLINE_KEY, null)
        if (isEmptyJsonList(value = videosJson)) return 0
        
        val repository = sharedOfflineRepo()
        var count = 0
        
        capture("Error migrating offline videos") {
            val jsonArray = JSONArray(videosJson)
            for (i in 0 until jsonArray.length()) {
                val videoJson = jsonArray.getJSONObject(i)
                
                val video = OfflineVideoModel(
                    id = videoJson.getString("id"),
                    youtubeId = videoJson.optString("youtubeId").takeIf { it.isNotEmpty() },
                    title = videoJson.getString("title"),
                    channelName = videoJson.optString("channelName", "Unknown"),
                    localPath = videoJson.getString("localPath"),
                    thumbnailPath = videoJson.optString("thumbnailPath").takeIf { it.isNotEmpty() },
                    thumbnailUrl = videoJson.optString("thumbnailUrl").takeIf { it.isNotEmpty() },
                    duration = videoJson.optLong("duration", 0L),
                    fileSize = videoJson.optLong("fileSize", 0L),
                    category = videoJson.optString("category").takeIf { it.isNotEmpty() },
                    addedAt = videoJson.optLong("addedAt", System.currentTimeMillis())
                )
                
                repository.addVideo(video)
                count++
            }
        }
        
        return count
    }
    
    /**
     * Reset migration flag (for testing or re-migration)
     */
    fun resetMigrationFlag(context: Context) {
        val migrationPrefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        migrationPrefs.edit()
            .remove(KEY_MIGRATION_COMPLETED)
            .remove("migration_timestamp")
            .apply()
        log(message = "Migration flag reset")
    }
    
    /**
     * Check if migration has been completed
     */
    fun isMigrationCompleted(context: Context): Boolean {
        val migrationPrefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        return migrationPrefs.getBoolean(KEY_MIGRATION_COMPLETED, false)
    }
}

// MigrationResult + MigrationStats moved to commonMain (see MigrationModels.kt)
