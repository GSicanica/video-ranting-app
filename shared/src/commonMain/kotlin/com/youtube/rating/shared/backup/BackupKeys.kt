package com.youtube.rating.shared.backup

/**
 * Centralized JSON keys used in backup payloads.
 *
 * Keep these stable for backwards compatibility with existing backups.
 */
object BackupKeys {
    const val VERSION = "version"
    const val TIMESTAMP = "timestamp"
    const val APP_VERSION = "appVersion"
    const val EXPORT_DATE = "exportDate"

    const val NOTES = "notes"
    const val FAVORITES = "favorites"
    const val OFFLINE_VIDEOS = "offlineVideos"

    const val TASKS = "tasks"

    const val ROSARY_SESSIONS = "rosarySessions"
    const val ROSARY_SETTINGS = "rosarySettings"
    const val ROSARY_STATS = "rosaryStats"

    const val PREFERENCES = "preferences"
    const val GALLERY_IMAGES = "galleryImages"
    const val GALLERY_PINNED = "galleryPinned"
    const val SORT_BY = "sortBy"
    const val INSTALL_ID = "installId"
    const val USER_TOKEN = "userToken"

    const val FASTING_ENTRIES = "fastingEntries"
    const val FASTING_WEEKLY_GOAL = "fastingWeeklyGoal"
    const val FASTING_REMINDER = "fastingReminder"

    const val WATCH_HISTORY = "watchHistory"
    const val WATCH_HISTORY_ENABLED = "watchHistoryEnabled"

    const val APP_LANGUAGE = "appLanguage"
    const val CONTENT_LANGUAGES = "contentLanguages"

    const val BIBLE_HIGHLIGHTS = "bibleHighlights"
    const val BIBLE_HIGHLIGHT_TEXTS = "bibleHighlightTexts"
    const val BIBLE_READER_FONT_SP = "bibleReaderFontSp"

    const val BIBLE_PLANNER = "biblePlanner"
    const val BIBLE_STATS = "bibleStats"
    const val BIBLE_SEQUENTIAL_PROGRESS = "bibleSequentialProgress"

    const val SAVED_PSALMS = "savedPsalms"
    const val PSALM_HIGHLIGHTS = "psalmHighlights"
    const val CUSTOM_NOVENAS = "customNovenas"

    const val THEME_COLORS = "themeColors"
    const val HOME_SCREEN_STYLE = "homeScreenStyle"
    const val FAVORITE_CUSTOM_CATEGORIES = "favoriteCustomCategories"

    const val TRAINING_STATS_DAILY = "trainingStatsDaily"
    const val TRAINING_PRAYER_GOAL = "trainingPrayerGoal"

    const val SAINTS_JSON = "saintsJson"
}
