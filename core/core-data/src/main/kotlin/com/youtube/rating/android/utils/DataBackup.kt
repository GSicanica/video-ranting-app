package com.youtube.rating.android.utils

import android.content.Context
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.storage.TaskManager
import com.youtube.rating.android.storage.FastingManager
import com.youtube.rating.shared.backup.BackupKeys
import com.youtube.rating.shared.data.FavoritesRepository
import com.youtube.rating.shared.data.NotesRepository
import com.youtube.rating.shared.data.OfflineVideosRepository
import com.youtube.rating.shared.utils.LogConfig
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named
import com.youtube.rating.android.data.prefs.AdminPrefs
import com.youtube.rating.android.data.prefs.BackupPrefs
import com.youtube.rating.android.data.prefs.BiblePlannerPrefs
import com.youtube.rating.android.data.prefs.BibleReaderPrefs
import com.youtube.rating.android.data.prefs.BibleSequentialPrefs
import com.youtube.rating.android.data.prefs.BibleStatsPrefs
import com.youtube.rating.android.data.prefs.BrightnessPrefs
import com.youtube.rating.android.data.prefs.FavoritesPrefs
import com.youtube.rating.android.data.prefs.GalleryPrefs
import com.youtube.rating.android.data.prefs.GenericPrefs
import com.youtube.rating.android.data.prefs.HomePrefs
import com.youtube.rating.android.data.prefs.InstallPrefs
import com.youtube.rating.android.data.prefs.PsalmPrefs
import com.youtube.rating.android.data.prefs.SaintsPrefs
import com.youtube.rating.android.data.prefs.ThemePrefs
import com.youtube.rating.android.data.prefs.TrainingPrefs
import com.youtube.rating.android.data.prefs.VideoPrefs

/**
 * Handles backup and restore of all user data
 * This provides additional data safety beyond migration
 * 
 * ✅ FIXED: Uses Logger.error() instead of printStackTrace()
 */
object DataBackup {
    
    private const val TAG = "DataBackup"
    private const val BACKUP_DIR_NAME = "backups"
    private const val AUTO_BACKUP_PREFS = "auto_backup_prefs"
    private const val KEY_LAST_BACKUP = "last_backup_timestamp"
    private const val AUTO_BACKUP_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours

    private fun sharedNotesRepo(): NotesRepository =
        GlobalContext.get().get(qualifier = named("sharedNotesRepo"))

    private fun sharedFavoritesRepo(): FavoritesRepository =
        GlobalContext.get().get(qualifier = named("sharedFavoritesRepo"))

    private fun sharedOfflineRepo(): OfflineVideosRepository =
        GlobalContext.get().get(qualifier = named("sharedOfflineVideosRepo"))

    private fun taskManager(): TaskManager =
        GlobalContext.get().get()
    
    /**
     * Create a full backup of all data
     */
    suspend fun createBackup(context: Context): BackupResult {
        return try {
            val backupDir = File(context.filesDir, BACKUP_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "backup_${dateFormat.format(Date(timestamp))}.json"
            val backupFile = File(backupDir, fileName)
            
            val backupData = createBackupData(context = context)
            
            backupFile.writeText(backupData.toString(2))
            
            // Update last backup timestamp
            BackupPrefs.setLastBackupTimestamp(context, timestamp)
            
            // Clean up old backups (keep only last 5)
            cleanupOldBackups(backupDir, keepCount = 5)
            
            if (LogConfig.ENABLE_LOGS) {
                Logger.info(TAG, "Backup created successfully: ${backupFile.absolutePath}")
            }
            BackupResult.Success(backupFile.absolutePath, backupFile.length())
            
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // ✅ FIX: Use Logger instead of printStackTrace
            Logger.error(TAG, "Backup failed", e)
            BackupResult.Failed(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Restore data from a backup file
     */
    suspend fun restoreBackup(context: Context, backupFilePath: String): RestoreResult {
        return try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) {
                return RestoreResult.Failed("Backup file not found")
            }
            
            val backupJson = JSONObject(backupFile.readText())
            val stats = RestoreStats()
            
            // Restore notes (preserving original IDs)
            val notesArray = backupJson.optJSONArray(BackupKeys.NOTES)
            if (notesArray != null) {
                val notesRepo = sharedNotesRepo()
                BackupJsonCodec.notesFromJsonArray(notesArray).forEach { noteModel ->
                    notesRepo.importNote(noteModel)
                    stats.notesRestored++
                }
            }
            
            // Restore favorites (also restore local favorite type when available)
            val favoritesArray = backupJson.optJSONArray(BackupKeys.FAVORITES)
            if (favoritesArray != null) {
                val favoritesRepo = sharedFavoritesRepo()
                for (i in 0 until favoritesArray.length()) {
                    val fav = favoritesArray.getJSONObject(i)
                    val typeStr = fav.optString("type", "VIDEO")
                    val favoriteModel = BackupJsonCodec.favoriteFromJson(fav)
                    favoritesRepo.addFavorite(favoriteModel)

                    // Restore local favorite with type if provided
                    val localType = try {
                        com.youtube.rating.android.storage.FavoriteItemType.valueOf(typeStr)
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        com.youtube.rating.android.storage.FavoriteItemType.VIDEO
                    }

                    val localFav = com.youtube.rating.android.storage.FavoriteVideo(
                        videoId = favoriteModel.videoId,
                        title = favoriteModel.title,
                        thumbnail = favoriteModel.thumbnail,
                        channelName = favoriteModel.channelName,
                        avgLove = favoriteModel.avgLove,
                        avgFaith = favoriteModel.avgFaith,
                        avgHope = favoriteModel.avgHope,
                        totalRatings = favoriteModel.totalRatings,
                        category = favoriteModel.category,
                        timestamp = favoriteModel.timestamp,
                        type = localType
                    )
                    // Use sync method to ensure local cache reflects restored type
                    stats.favoritesRestored++
                }
            }
                
                // Restore offline videos metadata (files remain in place)
                val videosArray = backupJson.optJSONArray(BackupKeys.OFFLINE_VIDEOS)
                if (videosArray != null) {
                    val videosRepo = sharedOfflineRepo()
                    BackupJsonCodec.offlineVideosFromJsonArray(videosArray).forEach { videoModel ->
                        videosRepo.addVideo(videoModel)
                        stats.offlineVideosRestored++
                    }
                }
            // Restore rosary settings and stats if present
            val rosarySettings = backupJson.optJSONObject(BackupKeys.ROSARY_SETTINGS)
            if (rosarySettings != null) {
                val rosaryManager = com.youtube.rating.android.storage.RosaryManager.getInstance(context)
                rosaryManager.importSettings(rosarySettings)
            }

            val rosaryStats = backupJson.optJSONObject(BackupKeys.ROSARY_STATS)
            if (rosaryStats != null) {
                val rosaryManager = com.youtube.rating.android.storage.RosaryManager.getInstance(context)
                rosaryManager.importStats(rosaryStats)
            }

            // Restore Bible highlights and texts
            val bibleHighlightsArray = backupJson.optJSONArray(BackupKeys.BIBLE_HIGHLIGHTS)
            if (bibleHighlightsArray != null) {
                val highlights = mutableSetOf<String>()
                for (i in 0 until bibleHighlightsArray.length()) {
                    highlights.add(bibleHighlightsArray.getString(i))
                }
                BibleReaderPrefs.setBibleHighlights(context, highlights)
            }
            val bibleHighlightTextsObj = backupJson.optJSONObject(BackupKeys.BIBLE_HIGHLIGHT_TEXTS)
            if (bibleHighlightTextsObj != null) {
                val keys = bibleHighlightTextsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = bibleHighlightTextsObj.optString(k, "")
                    if (v.isNotBlank()) {
                        BibleReaderPrefs.putBibleHighlightText(context, k, v)
                    }
                }
            }
            // Restore Bible reader font size
            val bibleFontSp = backupJson.optInt(BackupKeys.BIBLE_READER_FONT_SP, -1)
            if (bibleFontSp in 14..30) {
                BibleReaderPrefs.setBibleReaderFontSp(context, bibleFontSp)
            }

            // Restore language settings
            val appLang = backupJson.optString(BackupKeys.APP_LANGUAGE, "")
            if (appLang.isNotBlank()) {
                val lm = com.youtube.rating.android.localization.LanguageManager(context)
                try {
                    lm.saveLanguage(com.youtube.rating.android.localization.Strings.Language.valueOf(appLang))
                } catch (e: Exception) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                }
            }

            val contentLangs = backupJson.optJSONArray(BackupKeys.CONTENT_LANGUAGES)
            if (contentLangs != null) {
                val clm = com.youtube.rating.android.localization.ContentLanguageManager(context)
                val langs = mutableSetOf<com.youtube.rating.android.localization.Strings.Language>()
                for (i in 0 until contentLangs.length()) {
                    try {
                        langs.add(com.youtube.rating.android.localization.Strings.Language.valueOf(contentLangs.getString(i)))
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    }
                }
                if (langs.isNotEmpty()) clm.saveContentLanguages(langs)
            }

            // Restore app preferences/settings
            val prefsJson = backupJson.optJSONObject(BackupKeys.PREFERENCES)
            if (prefsJson != null) {
                if (prefsJson.has("gridView")) {
                    HomePrefs.setGridView(context, prefsJson.optBoolean("gridView", true))
                }

                if (prefsJson.has("appBrightness")) {
                    BrightnessPrefs.setAppBrightness(context, prefsJson.optDouble("appBrightness", 1.0).toFloat())
                }

                // Skip admin mode restore for safety.

                if (prefsJson.has(BackupKeys.GALLERY_IMAGES)) {
                    GalleryPrefs.setGalleryImagesJson(
                        context,
                        prefsJson.optJSONArray(BackupKeys.GALLERY_IMAGES)?.toString() ?: "[]"
                    )
                }
                if (prefsJson.has(BackupKeys.GALLERY_PINNED)) {
                    GalleryPrefs.setGalleryPinnedJson(
                        context,
                        prefsJson.optJSONArray(BackupKeys.GALLERY_PINNED)?.toString() ?: "[]"
                    )
                }

                val userToken = prefsJson.optString(BackupKeys.USER_TOKEN, "")
                if (userToken.isNotBlank()) {
                    // Note: userToken is now managed by UserTokenManager with encrypted storage
                    // We don't restore it directly as it should be obtained from server
                }

                val installId = prefsJson.optString(BackupKeys.INSTALL_ID, "")
                if (installId.isNotBlank()) {
                    InstallPrefs.setInstallId(context, installId)
                }

                val sortBy = prefsJson.optString(BackupKeys.SORT_BY, "")
                if (sortBy.isNotBlank()) {
                    HomePrefs.setSortBy(context, sortBy)
                }
            }

            // Restore tasks
            val tasksArray = backupJson.optJSONArray(BackupKeys.TASKS)
            if (tasksArray != null) {
                stats.tasksRestored = taskManager().importTasks(tasksArray)
            }

            // Restore fasting
            val fastingEntries = backupJson.optJSONArray(BackupKeys.FASTING_ENTRIES)
            if (fastingEntries != null) {
                val fastingManager = FastingManager.getInstance(context)
                stats.fastingRestored = fastingManager.importEntries(fastingEntries)
            }
            val fastingGoal = backupJson.optInt(BackupKeys.FASTING_WEEKLY_GOAL, -1)
            if (fastingGoal > 0) {
                FastingManager.getInstance(context).setWeeklyGoal(fastingGoal)
            }
            val fastingReminder = backupJson.optJSONObject(BackupKeys.FASTING_REMINDER)
            if (fastingReminder != null) {
                FastingManager.getInstance(context).saveReminderSettings(
                    BackupJsonCodec.fastingReminderFromJson(fastingReminder)
                )
            }

            // Restore Bible planner data
            val biblePlannerJson = backupJson.optJSONObject(BackupKeys.BIBLE_PLANNER)
            if (biblePlannerJson != null) {
                val streak = biblePlannerJson.optInt("streak", 0)
                if (streak > 0) BiblePlannerPrefs.setStreak(context, streak)
                val goalMin = biblePlannerJson.optInt("goalMin", -1)
                if (goalMin > 0) BiblePlannerPrefs.setGoal(context, goalMin)
                val note = biblePlannerJson.optString("note", "")
                if (note.isNotBlank()) BiblePlannerPrefs.setNote(context, note)
                val lastDate = biblePlannerJson.optString("lastDate", "")
                if (lastDate.isNotBlank()) BiblePlannerPrefs.setLastDate(context, lastDate)
                val reminderEnabled = biblePlannerJson.optBoolean("reminderEnabled", false)
                val reminderHour = biblePlannerJson.optInt("reminderHour", 8)
                val reminderMinute = biblePlannerJson.optInt("reminderMinute", 0)
                BiblePlannerPrefs.setReminderTime(context, reminderEnabled, reminderHour, reminderMinute)
            }

            // Restore Bible reading stats
            val bibleStatsJson = backupJson.optJSONObject(BackupKeys.BIBLE_STATS)
            if (bibleStatsJson != null) {
                val totalRead = bibleStatsJson.optInt("totalRead", 0)
                if (totalRead > 0) {
                    // Set directly by adding the difference
                    val currentTotal = BibleStatsPrefs.bibleTotalReadFlow(context).firstOrNull() ?: 0
                    if (totalRead > currentTotal) BibleStatsPrefs.addBibleReadTotal(context, totalRead - currentTotal)
                }
                val totalMeditation = bibleStatsJson.optInt("totalMeditation", 0)
                if (totalMeditation > 0) {
                    val currentMed = BibleStatsPrefs.bibleTotalMeditationFlow(context).firstOrNull() ?: 0
                    if (totalMeditation > currentMed) BibleStatsPrefs.addBibleMeditationTotal(context, totalMeditation - currentMed)
                }
                val bestStreak = bibleStatsJson.optInt("bestStreak", 0)
                if (bestStreak > 0) BibleStatsPrefs.updateBestStreak(context, bestStreak)
                val daysActive = bibleStatsJson.optInt("daysActive", 0)
                if (daysActive > 0) {
                    val currentDays = BibleStatsPrefs.bibleDaysActiveFlow(context).firstOrNull() ?: 0
                    if (daysActive > currentDays) {
                        repeat(daysActive - currentDays) { BibleStatsPrefs.addActiveDay(context) }
                    }
                }
                val dailyGoalUnits = bibleStatsJson.optInt("dailyGoalUnits", -1)
                if (dailyGoalUnits in 1..20) BibleStatsPrefs.setBibleDailyGoalUnits(context, dailyGoalUnits)
            }

            // Restore Bible sequential reading progress
            val seqJson = backupJson.optJSONObject(BackupKeys.BIBLE_SEQUENTIAL_PROGRESS)
            if (seqJson != null) {
                val bookIndex = seqJson.optInt("bookIndex", 0)
                val chapter = seqJson.optInt("chapter", 1)
                BibleSequentialPrefs.setBibleSequentialProgress(context, bookIndex, chapter)
            }

            // Restore saved psalms
            val savedPsalmsArr = backupJson.optJSONArray(BackupKeys.SAVED_PSALMS)
            if (savedPsalmsArr != null) {
                val psalms = mutableSetOf<String>()
                for (i in 0 until savedPsalmsArr.length()) {
                    psalms.add(savedPsalmsArr.getString(i))
                }
                if (psalms.isNotEmpty()) PsalmPrefs.setSavedPsalms(context, psalms)
            }

            // Restore psalm highlights
            val psalmHighlightsArr = backupJson.optJSONArray(BackupKeys.PSALM_HIGHLIGHTS)
            if (psalmHighlightsArr != null) {
                val highlights = mutableSetOf<String>()
                for (i in 0 until psalmHighlightsArr.length()) {
                    highlights.add(psalmHighlightsArr.getString(i))
                }
                if (highlights.isNotEmpty()) PsalmPrefs.setHighlightedPsalmLines(context, highlights)
            }

            // Restore custom novenas
            val customNovenasArr = backupJson.optJSONArray(BackupKeys.CUSTOM_NOVENAS)
            if (customNovenasArr != null) {
                for (i in 0 until customNovenasArr.length()) {
                    val name = customNovenasArr.optString(i, "")
                    if (name.isNotBlank()) TrainingPrefs.addCustomNovena(context, name)
                }
            }

            // Restore theme colors
            val themeJson = backupJson.optJSONObject(BackupKeys.THEME_COLORS)
            if (themeJson != null) {
                if (themeJson.has("primary")) ThemePrefs.setThemePrimary(context, themeJson.getInt("primary"))
                if (themeJson.has("secondary")) ThemePrefs.setThemeSecondary(context, themeJson.getInt("secondary"))
                if (themeJson.has("tertiary")) ThemePrefs.setThemeTertiary(context, themeJson.getInt("tertiary"))
            }

            // Restore home screen style
            val homeScreenStyle = backupJson.optString(BackupKeys.HOME_SCREEN_STYLE, "")
            if (homeScreenStyle.isNotBlank()) ThemePrefs.setHomeScreenStyle(context, homeScreenStyle)

            // Restore favorite custom categories
            val favCategoriesArr = backupJson.optJSONArray(BackupKeys.FAVORITE_CUSTOM_CATEGORIES)
            if (favCategoriesArr != null) {
                val categories = mutableSetOf<String>()
                for (i in 0 until favCategoriesArr.length()) {
                    val cat = favCategoriesArr.optString(i, "")
                    if (cat.isNotBlank()) categories.add(cat)
                }
                if (categories.isNotEmpty()) FavoritesPrefs.setFavoriteCustomCategories(context, categories)
            }

            // Restore training stats
            val trainingStatsDaily = backupJson.optString(BackupKeys.TRAINING_STATS_DAILY, "")
            if (trainingStatsDaily.isNotBlank() && trainingStatsDaily != "{}") {
                GenericPrefs.setString(context, "training_stats_daily", trainingStatsDaily)
            }
            val trainingPrayerGoal = backupJson.optInt(BackupKeys.TRAINING_PRAYER_GOAL, -1)
            if (trainingPrayerGoal > 0) TrainingPrefs.setTrainingPrayerGoal(context, trainingPrayerGoal)

            // Restore saints
            val saintsJson = backupJson.optString(BackupKeys.SAINTS_JSON, "")
            if (saintsJson.isNotBlank() && saintsJson != "[]") {
                SaintsPrefs.setSaintsJson(context, saintsJson)
            }

            // Restore watch history
            val watchHistoryArray = backupJson.optJSONArray(BackupKeys.WATCH_HISTORY)
            if (watchHistoryArray != null) {
                val watchHistoryManager = com.youtube.rating.android.storage.WatchHistoryManager.getInstance(context)

                for (i in 0 until watchHistoryArray.length()) {
                    try {
                        val entry = watchHistoryArray.getJSONObject(i)
                        val historyEntry = com.youtube.rating.android.storage.WatchHistoryEntry(
                            videoId = entry.getString("videoId"),
                            title = entry.getString("title"),
                            thumbnail = entry.optString("thumbnail", ""),
                            channelName = entry.optString("channelName", ""),
                            category = entry.optString("category").takeIf { it.isNotEmpty() },
                            watchDuration = entry.optLong("watchDuration", 0L),
                            totalDuration = entry.optLong("totalDuration", 0L),
                            viewedAt = entry.getLong("viewedAt")
                        )
                        watchHistoryManager.addToHistory(historyEntry)
                        stats.watchHistoryRestored++
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        Logger.error(TAG, "Error restoring watch history entry at index $i", e)
                    }
                }
            }

            // Restore watch history enabled setting
            if (backupJson.has(BackupKeys.WATCH_HISTORY_ENABLED)) {
                val historyEnabled = backupJson.optBoolean(BackupKeys.WATCH_HISTORY_ENABLED, true)
                VideoPrefs.setWatchHistoryEnabled(context, historyEnabled)
            }

            if (LogConfig.ENABLE_LOGS) {
                Logger.info(TAG, "Restore completed successfully: $stats")
            }
            RestoreResult.Success(stats)

        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // ✅ FIX: Use Logger instead of printStackTrace
            Logger.error(TAG, "Restore failed", e)
            RestoreResult.Failed(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Create backup data as JSON (public for AutoBackupManager)
     */
    suspend fun createBackupData(context: Context? = null): JSONObject {
        val backup = JSONObject()
        
        // Backup version and timestamp
        backup.put(BackupKeys.VERSION, 1)
        backup.put(BackupKeys.TIMESTAMP, System.currentTimeMillis())
        backup.put(BackupKeys.APP_VERSION, BuildConfig.VERSION_NAME)
        
        // Backup notes
        val notesRepo = sharedNotesRepo()
        val notesArray = BackupJsonCodec.notesToJsonArray(
            notesRepo.getAllNotes().firstOrNull().orEmpty()
        )
        // Debug: log note count
        if (com.youtube.rating.shared.utils.LogConfig.ENABLE_LOGS) {
            com.youtube.rating.shared.utils.Logger.info(TAG, "Backing up notes: ${notesArray.length()}")
        }
        backup.put(BackupKeys.NOTES, notesArray)
        
        // Backup favorites
        val favoritesRepo = sharedFavoritesRepo()
        val favoritesArray = BackupJsonCodec.favoritesToJsonArray(
            favoritesRepo.getAllFavorites().firstOrNull().orEmpty()
        )
        backup.put(BackupKeys.FAVORITES, favoritesArray)
        
        // Backup offline videos metadata
        val videosRepo = sharedOfflineRepo()
        val videosArray = BackupJsonCodec.offlineVideosToJsonArray(
            videosRepo.getAllVideos().firstOrNull().orEmpty()
        )
        // Debug: log offline videos count
        if (com.youtube.rating.shared.utils.LogConfig.ENABLE_LOGS) {
            com.youtube.rating.shared.utils.Logger.info(TAG, "Backing up offlineVideos: ${videosArray.length()}")
        }
        backup.put(BackupKeys.OFFLINE_VIDEOS, videosArray)

        // Backup tasks
        val tasksArray = JSONArray()
        context?.let { ctx ->
            val exported = taskManager().exportTasks()
            for (i in 0 until exported.length()) {
                tasksArray.put(exported.getJSONObject(i))
            }
        }
        backup.put(BackupKeys.TASKS, tasksArray)

        // Backup fasting
        context?.let { ctx ->
            val fastingManager = FastingManager.getInstance(ctx)
            backup.put(BackupKeys.FASTING_ENTRIES, fastingManager.exportEntries())
            backup.put(BackupKeys.FASTING_WEEKLY_GOAL, fastingManager.getWeeklyGoal())
            backup.put(BackupKeys.FASTING_REMINDER, BackupJsonCodec.fastingReminderToJson(fastingManager.getReminderSettings()))
        }
        
        // Backup rosary sessions, settings and stats (optional if context provided)
        context?.let { ctx ->
            val rosaryManager = com.youtube.rating.android.storage.RosaryManager.getInstance(ctx)
            backup.put(BackupKeys.ROSARY_SESSIONS, rosaryManager.exportSessions())
            // settings
            val settings = rosaryManager.settings.value
            val settingsJson = org.json.JSONObject().apply {
                put("enabled", settings.enabled)
                put("hour", settings.hour)
                put("minute", settings.minute)
                put("daysOfWeek", org.json.JSONArray(settings.daysOfWeek.toList()))
            }
            backup.put(BackupKeys.ROSARY_SETTINGS, settingsJson)

            // stats
            val stats = rosaryManager.stats.value
            val statsJson = org.json.JSONObject().apply {
                put("totalSessions", stats.totalSessions)
                put("completedSessions", stats.completedSessions)
                put("currentStreak", stats.currentStreak)
                put("longestStreak", stats.longestStreak)
                stats.lastPrayedDate?.let { put("lastPrayedDate", it) }
                val sessionsObj = org.json.JSONObject()
                stats.sessionsPerMystery.forEach { (k, v) -> sessionsObj.put(k, v) }
                put("sessionsPerMystery", sessionsObj)
            }
            backup.put(BackupKeys.ROSARY_STATS, statsJson)
        }

        // Backup app language and selected content languages
        context?.let { ctx ->
            val lm = com.youtube.rating.android.localization.LanguageManager(ctx)
            val appLang = lm.loadLanguage()
            backup.put(BackupKeys.APP_LANGUAGE, appLang.name)

            val clm = com.youtube.rating.android.localization.ContentLanguageManager(ctx)
            val contentLangs = clm.getSelectedContentLanguages().map { it.name }
            backup.put(BackupKeys.CONTENT_LANGUAGES, org.json.JSONArray(contentLangs))
        }

        // Backup Bible highlights and their texts
        context?.let { ctx ->
            val highlights = BibleReaderPrefs.bibleHighlightsFlow(ctx).firstOrNull().orEmpty()
            backup.put(BackupKeys.BIBLE_HIGHLIGHTS, JSONArray(highlights.toList()))
            val highlightTexts = BibleReaderPrefs.bibleHighlightTextsFlow(ctx).firstOrNull().orEmpty()
            val textsObj = JSONObject()
            highlightTexts.forEach { (k, v) -> textsObj.put(k, v) }
            backup.put(BackupKeys.BIBLE_HIGHLIGHT_TEXTS, textsObj)
            // Also backup Bible reader font size
            val fontSp = BibleReaderPrefs.bibleReaderFontSpFlow(ctx).firstOrNull()
            if (fontSp != null) backup.put(BackupKeys.BIBLE_READER_FONT_SP, fontSp)
        }

        // Backup Bible planner data
        context?.let { ctx ->
            val biblePlannerJson = org.json.JSONObject()
            biblePlannerJson.put("streak", BiblePlannerPrefs.streakFlow(ctx).firstOrNull() ?: 0)
            biblePlannerJson.put("goalMin", BiblePlannerPrefs.goalFlow(ctx).firstOrNull() ?: 0)
            biblePlannerJson.put("note", BiblePlannerPrefs.noteFlow(ctx).firstOrNull() ?: "")
            biblePlannerJson.put("lastDate", BiblePlannerPrefs.lastDateFlow(ctx).firstOrNull() ?: "")
            biblePlannerJson.put("reminderEnabled", BiblePlannerPrefs.getReminderEnabled(ctx))
            biblePlannerJson.put("reminderHour", BiblePlannerPrefs.getReminderHour(ctx))
            biblePlannerJson.put("reminderMinute", BiblePlannerPrefs.getReminderMinute(ctx))
            backup.put(BackupKeys.BIBLE_PLANNER, biblePlannerJson)
        }

        // Backup Bible reading stats
        context?.let { ctx ->
            val bibleStatsJson = org.json.JSONObject()
            bibleStatsJson.put("totalRead", BibleStatsPrefs.bibleTotalReadFlow(ctx).firstOrNull() ?: 0)
            bibleStatsJson.put("totalMeditation", BibleStatsPrefs.bibleTotalMeditationFlow(ctx).firstOrNull() ?: 0)
            bibleStatsJson.put("bestStreak", BibleStatsPrefs.bibleBestStreakFlow(ctx).firstOrNull() ?: 0)
            bibleStatsJson.put("daysActive", BibleStatsPrefs.bibleDaysActiveFlow(ctx).firstOrNull() ?: 0)
            bibleStatsJson.put("dailyGoalUnits", BibleStatsPrefs.bibleDailyGoalUnitsFlow(ctx).firstOrNull() ?: 0)
            backup.put(BackupKeys.BIBLE_STATS, bibleStatsJson)
        }

        // Backup Bible sequential reading progress
        context?.let { ctx ->
            val seqJson = org.json.JSONObject()
            seqJson.put("bookIndex", BibleSequentialPrefs.bibleSequentialBookIndexFlow(ctx).firstOrNull() ?: 0)
            seqJson.put("chapter", BibleSequentialPrefs.bibleSequentialChapterFlow(ctx).firstOrNull() ?: 1)
            backup.put(BackupKeys.BIBLE_SEQUENTIAL_PROGRESS, seqJson)
        }

        // Backup saved psalms
        context?.let { ctx ->
            val savedPsalms = PsalmPrefs.savedPsalmsFlow(ctx).firstOrNull().orEmpty()
            backup.put(BackupKeys.SAVED_PSALMS, JSONArray(savedPsalms.toList()))
        }

        // Backup psalm highlights
        context?.let { ctx ->
            val psalmHighlights = PsalmPrefs.highlightedPsalmLinesFlow(ctx).firstOrNull().orEmpty()
            backup.put(BackupKeys.PSALM_HIGHLIGHTS, JSONArray(psalmHighlights.toList()))
        }

        // Backup custom novenas
        context?.let { ctx ->
            val customNovenas = TrainingPrefs.customNovenasFlow(ctx).firstOrNull().orEmpty()
            backup.put(BackupKeys.CUSTOM_NOVENAS, JSONArray(customNovenas.toList()))
        }

        // Backup theme colors
        context?.let { ctx ->
            val themePrefs = ThemePrefs.themePrefsFlow(ctx).firstOrNull()
            if (themePrefs != null) {
                val themeJson = org.json.JSONObject()
                themePrefs.primary?.let { themeJson.put("primary", it) }
                themePrefs.secondary?.let { themeJson.put("secondary", it) }
                themePrefs.tertiary?.let { themeJson.put("tertiary", it) }
                backup.put(BackupKeys.THEME_COLORS, themeJson)
            }
        }

        // Backup home screen style
        context?.let { ctx ->
            backup.put(BackupKeys.HOME_SCREEN_STYLE, ThemePrefs.homeScreenStyleFlow(ctx).firstOrNull() ?: "default")
        }

        // Backup favorite custom categories
        context?.let { ctx ->
            val categories = FavoritesPrefs.favoriteCustomCategoriesFlow(ctx).firstOrNull().orEmpty()
            backup.put(BackupKeys.FAVORITE_CUSTOM_CATEGORIES, JSONArray(categories.toList()))
        }

        // Backup training stats
        context?.let { ctx ->
            val trainingStatsJson = TrainingPrefs.trainingStatsJsonFlow(ctx).firstOrNull() ?: "{}"
            backup.put(BackupKeys.TRAINING_STATS_DAILY, trainingStatsJson)
            backup.put(BackupKeys.TRAINING_PRAYER_GOAL, TrainingPrefs.trainingPrayerGoalFlow(ctx).firstOrNull() ?: 0)
        }

        // Backup saints
        context?.let { ctx ->
            backup.put(BackupKeys.SAINTS_JSON, SaintsPrefs.getSaintsJson(ctx))
        }

        // Backup settings/preferences (metadata only)
        context?.let { ctx ->
            val prefsJson = org.json.JSONObject()

            prefsJson.put("gridView", HomePrefs.isGridView(ctx))
            prefsJson.put("appBrightness", BrightnessPrefs.getAppBrightness(ctx, 1.0f))
            prefsJson.put("adminMode", AdminPrefs.getAdminMode(ctx))
            prefsJson.put(BackupKeys.GALLERY_IMAGES, org.json.JSONArray(GalleryPrefs.getGalleryImagesJson(ctx)))
            prefsJson.put(BackupKeys.GALLERY_PINNED, org.json.JSONArray(GalleryPrefs.getGalleryPinnedJson(ctx)))
            // Do not backup auth token: avoids global app singleton coupling and credential export.
            prefsJson.put(BackupKeys.USER_TOKEN, "")
            prefsJson.put(BackupKeys.INSTALL_ID, InstallPrefs.getInstallId(ctx))

            val sortBy = HomePrefs.getSortBy(ctx)
            if (!sortBy.isNullOrBlank()) prefsJson.put(BackupKeys.SORT_BY, sortBy)

            backup.put(BackupKeys.PREFERENCES, prefsJson)
        }

        // Backup watch history
        context?.let { ctx ->
            val watchHistoryManager = com.youtube.rating.android.storage.WatchHistoryManager.getInstance(ctx)
            val history = watchHistoryManager.getHistory()
            val historyArray = JSONArray()

            history.forEach { entry ->
                historyArray.put(JSONObject().apply {
                    put("videoId", entry.videoId)
                    put("title", entry.title)
                    put("thumbnail", entry.thumbnail)
                    put("channelName", entry.channelName)
                    put("category", entry.category ?: "")
                    put("watchDuration", entry.watchDuration)
                    put("totalDuration", entry.totalDuration)
                    put("viewedAt", entry.viewedAt)
                })
            }

            backup.put(BackupKeys.WATCH_HISTORY, historyArray)

            // Also backup watch history enabled setting
            val historyEnabled = VideoPrefs.watchHistoryEnabledFlow(ctx).firstOrNull() ?: true
            backup.put(BackupKeys.WATCH_HISTORY_ENABLED, historyEnabled)

            if (com.youtube.rating.shared.utils.LogConfig.ENABLE_LOGS) {
                com.youtube.rating.shared.utils.Logger.info(TAG, "Backing up watch history: ${historyArray.length()} entries")
            }
        }

        return backup
    }
    
    /**
     * Async restore from JSON backup data (for AutoBackupManager)
     */
    suspend fun restoreFromBackupAsync(context: Context, backupJson: String): RestoreStats {
        val backup = JSONObject(backupJson)
        val stats = RestoreStats()
        
        // Restore notes (preserving original IDs)
        val notesArray = backup.optJSONArray(BackupKeys.NOTES)
        if (notesArray != null) {
            val notesRepo = sharedNotesRepo()
            BackupJsonCodec.notesFromJsonArray(notesArray).forEach { noteModel ->
                notesRepo.importNote(noteModel)
                stats.notesRestored++
            }
        }

        // Restore favorites
        val favoritesArray = backup.optJSONArray(BackupKeys.FAVORITES)
        if (favoritesArray != null) {
            val favoritesRepo = sharedFavoritesRepo()
            for (i in 0 until favoritesArray.length()) {
                val fav = favoritesArray.getJSONObject(i)
                val typeStr = fav.optString("type", "VIDEO")
                val favoriteModel = BackupJsonCodec.favoriteFromJson(fav)
                favoritesRepo.addFavorite(favoriteModel)
                // restore local type if present
                val localType = try {
                    com.youtube.rating.android.storage.FavoriteItemType.valueOf(typeStr)
                } catch (e: Exception) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    com.youtube.rating.android.storage.FavoriteItemType.VIDEO
                }
                val localFav = com.youtube.rating.android.storage.FavoriteVideo(
                    videoId = favoriteModel.videoId,
                    title = favoriteModel.title,
                    thumbnail = favoriteModel.thumbnail,
                    channelName = favoriteModel.channelName,
                    avgLove = favoriteModel.avgLove,
                    avgFaith = favoriteModel.avgFaith,
                    avgHope = favoriteModel.avgHope,
                    totalRatings = favoriteModel.totalRatings,
                    category = favoriteModel.category,
                    timestamp = favoriteModel.timestamp,
                    type = localType
                )
                // local cache will be refreshed by FavoritesViewModel observing the repository
                stats.favoritesRestored++
            }
        }
        
        // Restore offline videos
        val videosArray = backup.optJSONArray(BackupKeys.OFFLINE_VIDEOS)
        if (videosArray != null) {
            val videosRepo = sharedOfflineRepo()
            BackupJsonCodec.offlineVideosFromJsonArray(videosArray).forEach { videoModel ->
                videosRepo.addVideo(videoModel)
                stats.offlineVideosRestored++
            }
        }
        
        // Restore rosary sessions
        val rosarySessionsArray = backup.optJSONArray(BackupKeys.ROSARY_SESSIONS)
        if (rosarySessionsArray != null) {
            val rosaryManager = com.youtube.rating.android.storage.RosaryManager.getInstance(context)
            rosaryManager.importSessions(rosarySessionsArray)
            stats.rosarySessionsRestored = rosarySessionsArray.length()
        }

        // Restore rosary settings
        val rosarySettingsObj = backup.optJSONObject(BackupKeys.ROSARY_SETTINGS)
        if (rosarySettingsObj != null) {
            val rosaryManager = com.youtube.rating.android.storage.RosaryManager.getInstance(context)
            rosaryManager.importSettings(rosarySettingsObj)
        }

        // Restore rosary stats
        val rosaryStatsObj = backup.optJSONObject(BackupKeys.ROSARY_STATS)
        if (rosaryStatsObj != null) {
            val rosaryManager = com.youtube.rating.android.storage.RosaryManager.getInstance(context)
            rosaryManager.importStats(rosaryStatsObj)
        }

        // Restore Bible highlights and texts
        val bibleHighlightsArr = backup.optJSONArray(BackupKeys.BIBLE_HIGHLIGHTS)
        if (bibleHighlightsArr != null) {
            val highlights = mutableSetOf<String>()
            for (i in 0 until bibleHighlightsArr.length()) {
                highlights.add(bibleHighlightsArr.getString(i))
            }
            BibleReaderPrefs.setBibleHighlights(context, highlights)
        }
        val bibleHighlightTextsAsync = backup.optJSONObject(BackupKeys.BIBLE_HIGHLIGHT_TEXTS)
        if (bibleHighlightTextsAsync != null) {
            val keys = bibleHighlightTextsAsync.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = bibleHighlightTextsAsync.optString(k, "")
                if (v.isNotBlank()) {
                    BibleReaderPrefs.putBibleHighlightText(context, k, v)
                }
            }
        }
        // Restore Bible reader font size
        val bibleFontSpAsync = backup.optInt(BackupKeys.BIBLE_READER_FONT_SP, -1)
        if (bibleFontSpAsync in 14..30) {
            BibleReaderPrefs.setBibleReaderFontSp(context, bibleFontSpAsync)
        }

        // Restore app language and content languages
        val appLang = backup.optString(BackupKeys.APP_LANGUAGE, "")
        if (appLang.isNotBlank()) {
            val lm = com.youtube.rating.android.localization.LanguageManager(context)
            try {
                lm.saveLanguage(com.youtube.rating.android.localization.Strings.Language.valueOf(appLang))
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                // ignore unknown language
            }
        }

        val contentLangsArr = backup.optJSONArray(BackupKeys.CONTENT_LANGUAGES)
        if (contentLangsArr != null) {
            val clm = com.youtube.rating.android.localization.ContentLanguageManager(context)
            val langs = mutableSetOf<com.youtube.rating.android.localization.Strings.Language>()
            for (i in 0 until contentLangsArr.length()) {
                val name = contentLangsArr.optString(i)
                try {
                    val l = com.youtube.rating.android.localization.Strings.Language.valueOf(name)
                    langs.add(l)
                } catch (e: Exception) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    // skip
                }
            }
            if (langs.isNotEmpty()) clm.saveContentLanguages(langs)
        }

        // Restore preferences/settings
        val prefsJson = backup.optJSONObject(BackupKeys.PREFERENCES)
        if (prefsJson != null) {
            if (prefsJson.has("gridView")) {
                HomePrefs.setGridView(context, prefsJson.optBoolean("gridView", true))
            }

            if (prefsJson.has("appBrightness")) {
                BrightnessPrefs.setAppBrightness(context, prefsJson.optDouble("appBrightness", 1.0).toFloat())
            }

            // Skip admin mode restore for safety.

            if (prefsJson.has(BackupKeys.GALLERY_IMAGES)) {
                GalleryPrefs.setGalleryImagesJson(
                    context,
                    prefsJson.optJSONArray(BackupKeys.GALLERY_IMAGES)?.toString() ?: "[]"
                )
            }
            if (prefsJson.has(BackupKeys.GALLERY_PINNED)) {
                GalleryPrefs.setGalleryPinnedJson(
                    context,
                    prefsJson.optJSONArray(BackupKeys.GALLERY_PINNED)?.toString() ?: "[]"
                )
            }

            // Do not restore device/install IDs to avoid identity collisions

            val sortBy = prefsJson.optString(BackupKeys.SORT_BY, "")
            if (sortBy.isNotBlank()) {
                HomePrefs.setSortBy(context, sortBy)
            }
        }

        // Restore tasks
        val tasksArray = backup.optJSONArray(BackupKeys.TASKS)
        if (tasksArray != null) {
            stats.tasksRestored = taskManager().importTasks(tasksArray)
        }

        // Restore fasting
        val fastingEntries = backup.optJSONArray(BackupKeys.FASTING_ENTRIES)
        if (fastingEntries != null) {
            val fastingManager = FastingManager.getInstance(context)
            stats.fastingRestored = fastingManager.importEntries(fastingEntries)
        }
        val fastingGoal = backup.optInt(BackupKeys.FASTING_WEEKLY_GOAL, -1)
        if (fastingGoal > 0) {
            FastingManager.getInstance(context).setWeeklyGoal(fastingGoal)
        }
        val fastingReminder = backup.optJSONObject(BackupKeys.FASTING_REMINDER)
        if (fastingReminder != null) {
            val manager = FastingManager.getInstance(context)
            manager.saveReminderSettings(BackupJsonCodec.fastingReminderFromJson(fastingReminder))
        }

        // Restore Bible planner data
        val biblePlannerJson = backup.optJSONObject(BackupKeys.BIBLE_PLANNER)
        if (biblePlannerJson != null) {
            val streak = biblePlannerJson.optInt("streak", 0)
            if (streak > 0) BiblePlannerPrefs.setStreak(context, streak)
            val goalMin = biblePlannerJson.optInt("goalMin", -1)
            if (goalMin > 0) BiblePlannerPrefs.setGoal(context, goalMin)
            val plannerNote = biblePlannerJson.optString("note", "")
            if (plannerNote.isNotBlank()) BiblePlannerPrefs.setNote(context, plannerNote)
            val lastDate = biblePlannerJson.optString("lastDate", "")
            if (lastDate.isNotBlank()) BiblePlannerPrefs.setLastDate(context, lastDate)
            val reminderEnabled = biblePlannerJson.optBoolean("reminderEnabled", false)
            val reminderHour = biblePlannerJson.optInt("reminderHour", 8)
            val reminderMinute = biblePlannerJson.optInt("reminderMinute", 0)
            BiblePlannerPrefs.setReminderTime(context, reminderEnabled, reminderHour, reminderMinute)
        }

        // Restore Bible reading stats
        val bibleStatsJson = backup.optJSONObject(BackupKeys.BIBLE_STATS)
        if (bibleStatsJson != null) {
            val totalRead = bibleStatsJson.optInt("totalRead", 0)
            if (totalRead > 0) {
                val currentTotal = BibleStatsPrefs.bibleTotalReadFlow(context).firstOrNull() ?: 0
                if (totalRead > currentTotal) BibleStatsPrefs.addBibleReadTotal(context, totalRead - currentTotal)
            }
            val totalMeditation = bibleStatsJson.optInt("totalMeditation", 0)
            if (totalMeditation > 0) {
                val currentMed = BibleStatsPrefs.bibleTotalMeditationFlow(context).firstOrNull() ?: 0
                if (totalMeditation > currentMed) BibleStatsPrefs.addBibleMeditationTotal(context, totalMeditation - currentMed)
            }
            val bestStreak = bibleStatsJson.optInt("bestStreak", 0)
            if (bestStreak > 0) BibleStatsPrefs.updateBestStreak(context, bestStreak)
            val daysActive = bibleStatsJson.optInt("daysActive", 0)
            if (daysActive > 0) {
                val currentDays = BibleStatsPrefs.bibleDaysActiveFlow(context).firstOrNull() ?: 0
                if (daysActive > currentDays) {
                    repeat(daysActive - currentDays) { BibleStatsPrefs.addActiveDay(context) }
                }
            }
            val dailyGoalUnits = bibleStatsJson.optInt("dailyGoalUnits", -1)
            if (dailyGoalUnits in 1..20) BibleStatsPrefs.setBibleDailyGoalUnits(context, dailyGoalUnits)
        }

        // Restore Bible sequential reading progress
        val seqJson = backup.optJSONObject(BackupKeys.BIBLE_SEQUENTIAL_PROGRESS)
        if (seqJson != null) {
            val bookIndex = seqJson.optInt("bookIndex", 0)
            val chapter = seqJson.optInt("chapter", 1)
            BibleSequentialPrefs.setBibleSequentialProgress(context, bookIndex, chapter)
        }

        // Restore saved psalms
        val savedPsalmsArr = backup.optJSONArray(BackupKeys.SAVED_PSALMS)
        if (savedPsalmsArr != null) {
            val psalms = mutableSetOf<String>()
            for (i in 0 until savedPsalmsArr.length()) {
                psalms.add(savedPsalmsArr.getString(i))
            }
            if (psalms.isNotEmpty()) PsalmPrefs.setSavedPsalms(context, psalms)
        }

        // Restore psalm highlights
        val psalmHighlightsArr = backup.optJSONArray(BackupKeys.PSALM_HIGHLIGHTS)
        if (psalmHighlightsArr != null) {
            val highlights = mutableSetOf<String>()
            for (i in 0 until psalmHighlightsArr.length()) {
                highlights.add(psalmHighlightsArr.getString(i))
            }
            if (highlights.isNotEmpty()) PsalmPrefs.setHighlightedPsalmLines(context, highlights)
        }

        // Restore custom novenas
        val customNovenasArr = backup.optJSONArray(BackupKeys.CUSTOM_NOVENAS)
        if (customNovenasArr != null) {
            for (i in 0 until customNovenasArr.length()) {
                val name = customNovenasArr.optString(i, "")
                if (name.isNotBlank()) TrainingPrefs.addCustomNovena(context, name)
            }
        }

        // Restore theme colors
        val themeJson = backup.optJSONObject(BackupKeys.THEME_COLORS)
        if (themeJson != null) {
            if (themeJson.has("primary")) ThemePrefs.setThemePrimary(context, themeJson.getInt("primary"))
            if (themeJson.has("secondary")) ThemePrefs.setThemeSecondary(context, themeJson.getInt("secondary"))
            if (themeJson.has("tertiary")) ThemePrefs.setThemeTertiary(context, themeJson.getInt("tertiary"))
        }

        // Restore home screen style
        val homeScreenStyle = backup.optString(BackupKeys.HOME_SCREEN_STYLE, "")
        if (homeScreenStyle.isNotBlank()) ThemePrefs.setHomeScreenStyle(context, homeScreenStyle)

        // Restore favorite custom categories
        val favCategoriesArr = backup.optJSONArray(BackupKeys.FAVORITE_CUSTOM_CATEGORIES)
        if (favCategoriesArr != null) {
            val categories = mutableSetOf<String>()
            for (i in 0 until favCategoriesArr.length()) {
                val cat = favCategoriesArr.optString(i, "")
                if (cat.isNotBlank()) categories.add(cat)
            }
            if (categories.isNotEmpty()) FavoritesPrefs.setFavoriteCustomCategories(context, categories)
        }

        // Restore training stats
        val trainingStatsDaily = backup.optString(BackupKeys.TRAINING_STATS_DAILY, "")
        if (trainingStatsDaily.isNotBlank() && trainingStatsDaily != "{}") {
            GenericPrefs.setString(context, "training_stats_daily", trainingStatsDaily)
        }
        val trainingPrayerGoal = backup.optInt(BackupKeys.TRAINING_PRAYER_GOAL, -1)
        if (trainingPrayerGoal > 0) TrainingPrefs.setTrainingPrayerGoal(context, trainingPrayerGoal)

        // Restore saints
        val saintsJson = backup.optString(BackupKeys.SAINTS_JSON, "")
        if (saintsJson.isNotBlank() && saintsJson != "[]") {
            SaintsPrefs.setSaintsJson(context, saintsJson)
        }

        // Restore watch history
        val watchHistoryArray = backup.optJSONArray(BackupKeys.WATCH_HISTORY)
        if (watchHistoryArray != null) {
            val watchHistoryManager = com.youtube.rating.android.storage.WatchHistoryManager.getInstance(context)

            for (i in 0 until watchHistoryArray.length()) {
                try {
                    val entry = watchHistoryArray.getJSONObject(i)
                    val historyEntry = com.youtube.rating.android.storage.WatchHistoryEntry(
                        videoId = entry.getString("videoId"),
                        title = entry.getString("title"),
                        thumbnail = entry.optString("thumbnail", ""),
                        channelName = entry.optString("channelName", ""),
                        category = entry.optString("category").takeIf { it.isNotEmpty() },
                        watchDuration = entry.optLong("watchDuration", 0L),
                        totalDuration = entry.optLong("totalDuration", 0L),
                        viewedAt = entry.getLong("viewedAt")
                    )
                    watchHistoryManager.addToHistory(historyEntry)
                    stats.watchHistoryRestored++
                } catch (e: Exception) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    Logger.error(TAG, "Error restoring watch history entry at index $i", e)
                }
            }
        }

        // Restore watch history enabled setting
        if (backup.has(BackupKeys.WATCH_HISTORY_ENABLED)) {
            val historyEnabled = backup.optBoolean(BackupKeys.WATCH_HISTORY_ENABLED, true)
            VideoPrefs.setWatchHistoryEnabled(context, historyEnabled)
        }

        return stats
    }
    
    /**
     * Create backup data as JSON string (for AutoBackupManager)
     */
    suspend fun createBackupDataAsync(context: Context): String {
        val backupData = createBackupData(context = context)
        return backupData.toString(2)
    }
    
    /**
     * Clean up old backup files, keeping only the most recent ones
     */
    private fun cleanupOldBackups(backupDir: File, keepCount: Int) {
        try {
            val backupFiles = backupDir.listFiles()?.filter { it.name.startsWith("backup_") }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            
            if (backupFiles.size > keepCount) {
                backupFiles.drop(keepCount).forEach { file ->
                    if (file.delete()) {
                        if (LogConfig.ENABLE_LOGS) {
                            Logger.debug(TAG, "Deleted old backup: ${file.name}")
                        }
                    } else {
                        Logger.error(TAG, "Failed to delete old backup: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // ✅ FIX: Use Logger instead of printStackTrace
            Logger.error(TAG, "Error cleaning up old backups", e)
        }
    }
    
    /**
     * Auto backup if needed (called on app start)
     * ✅ FIXED: Changed to suspend function to use structured concurrency
     * Call from existing coroutine scope instead of creating unbounded scope
     */
    suspend fun autoBackupIfNeeded(context: Context) {
        try {
            val legacyPrefs = context.getSharedPreferences(AUTO_BACKUP_PREFS, Context.MODE_PRIVATE)
            val legacyLastBackup = legacyPrefs.getLong(KEY_LAST_BACKUP, 0)
            if (legacyLastBackup > 0) {
                BackupPrefs.setLastBackupTimestamp(context, legacyLastBackup)
            }
            val lastBackup = BackupPrefs.getLastBackupTimestamp(context)
            val now = System.currentTimeMillis()
            
            if (now - lastBackup > AUTO_BACKUP_INTERVAL_MS) {
                if (LogConfig.ENABLE_LOGS) {
                    Logger.info(TAG, "Auto backup triggered")
                }
                createBackup(context = context)
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // ✅ FIX: Use Logger instead of printStackTrace
            Logger.error(TAG, "Auto backup failed", e)
        }
    }
    
    /**
     * Get list of available backups
     */
    fun getAvailableBackups(context: Context): List<BackupInfo> {
        val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!backupDir.exists()) return emptyList()
        
        return backupDir.listFiles()
            ?.filter { it.name.startsWith("backup_") && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                BackupInfo(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    timestamp = file.lastModified(),
                    size = file.length()
                )
            }
            ?: emptyList()
    }
}

/**
 * Information about a backup file
 */
data class BackupInfo(
    val filePath: String,
    val fileName: String,
    val timestamp: Long,
    val size: Long
) {
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    fun getFormattedSize(): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format("%.2f MB", mb)
            else -> String.format("%.1f KB", kb)
        }
    }
}
