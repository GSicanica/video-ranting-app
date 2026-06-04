package com.youtube.rating.android.localization

import com.youtube.rating.shared.l10n.AppLanguage
import com.youtube.rating.shared.l10n.L10n
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

object Strings {
    private val languageRef = AtomicReference(Language.ENGLISH)
    private val selectedContentLanguagesRef = AtomicReference(setOf(Language.ENGLISH))
    private val _currentLanguageFlow = MutableStateFlow(languageRef.get())
    private val _selectedContentLanguagesFlow = MutableStateFlow(selectedContentLanguagesRef.get())

    var currentLanguage: Language
        get() = languageRef.get()
        set(value) {
            languageRef.set(value)
            _currentLanguageFlow.value = value
            L10n.setLanguage(value.toAppLanguage())
        }

    var selectedContentLanguages: Set<Language>
        get() = selectedContentLanguagesRef.get()
        set(value) {
            selectedContentLanguagesRef.set(value)
            _selectedContentLanguagesFlow.value = value
        }

    val currentLanguageFlow: StateFlow<Language> = _currentLanguageFlow.asStateFlow()
    val selectedContentLanguagesFlow: StateFlow<Set<Language>> =
        _selectedContentLanguagesFlow.asStateFlow()

    enum class Language {
        ENGLISH,
        CROATIAN,
        GERMAN
    }

    private fun Language.toAppLanguage(): AppLanguage = when (this) {
        Language.ENGLISH -> AppLanguage.ENGLISH
        Language.CROATIAN -> AppLanguage.CROATIAN
        Language.GERMAN -> AppLanguage.GERMAN
    }

    val home: String
        get() = L10n.t("home")

    val search: String
        get() = L10n.t("search")

    val favorites: String
        get() = L10n.t("favorites")

    val prayer: String
        get() = L10n.t("prayer")

    val rated: String
        get() = L10n.t("rated")

    val featured: String
        get() = L10n.t("featured")

    val browse: String
        get() = L10n.t("browse")

    val homeTitle: String
        get() = L10n.t("home_title")

    val youtubeLink: String
        get() = L10n.t("youtube_link")

    val loadVideo: String
        get() = L10n.t("load_video")

    val sendRating: String
        get() = L10n.t("send_rating")

    val sending: String
        get() = L10n.t("sending")

    val selectCategory: String
        get() = L10n.t("select_category")

    val noCategory: String
        get() = L10n.t("no_category")

    val enterYoutubeLinkAbove: String
        get() = L10n.t("enter_youtube_link_above")

    val ratingSuccess: String
        get() = L10n.t("rating_success")

    val invalidYoutubeUrl: String
        get() = L10n.t("invalid_youtube_url")

    val Thanks: String
        get() = L10n.t("thanks")

    val youtubeVideo: String
        get() = L10n.t("youtube_video")

    val teaching: String
        get() = L10n.t("teaching")

    val testimony: String
        get() = L10n.t("testimony")

    val bible: String
        get() = L10n.t("bible")

    val markOfBeast: String
        get() = L10n.t("mark_of_beast")

    val all: String
        get() = L10n.t("all")

    val love: String
        get() = L10n.t("love")

    val faith: String
        get() = L10n.t("faith")

    val hope: String
        get() = L10n.t("hope")

    val searchTitle: String
        get() = L10n.t("search_title")

    val searchPlaceholder: String
        get() = L10n.t("search_placeholder")

    val minRatings: String
        get() = L10n.t("min_ratings")

    val minimumRatings: String
        get() = L10n.t("minimum_ratings")

    val tryDifferentFilters: String
        get() = L10n.t("try_different_filters")

    val clickForDetails: String
        get() = L10n.t("click_for_details")

    val noRatedVideos: String
        get() = L10n.t("no_rated_videos")

    val filters: String
        get() = L10n.t("filters")

    val cancel: String
        get() = L10n.t("cancel")

    val sortBy: String
        get() = L10n.t("sort_by")

    val latest: String
        get() = L10n.t("latest")

    val noResults: String
        get() = L10n.t("no_results")

    val tryDifferentSearch: String
        get() = L10n.t("try_different_search")

    val myFavorites: String
        get() = L10n.t("my_favorites")

    val savedîtems: String
        get() = L10n.t("saveditems")

    val deleteAll: String
        get() = L10n.t("delete_all")

    val noFavorites: String
        get() = L10n.t("no_favorites")

    val addVideosToFavorites: String
        get() = L10n.t("add_videos_to_favorites")

    val addFavoritesMessage: String
        get() = L10n.t("add_favorites_message")

    val removeFromFavorites: String
        get() = L10n.t("remove_from_favorites")

    val removeFromFavoritesMessage: String
        get() = L10n.t("remove_from_favorites_message")

    val remove: String
        get() = L10n.t("remove")

    val addedToFavorites: String
        get() = L10n.t("added_to_favorites")

    val removedFromFavorites: String
        get() = L10n.t("removed_from_favorites")

    val ratedVideosTitle: String
        get() = L10n.t("rated_videos_title")

    val totalRatings: String
        get() = L10n.t("total_ratings")

    val andMore: String
        get() = L10n.t("and_more")

    val noVideosRated: String
        get() = L10n.t("no_videos_rated")

    val beFirstToRate: String
        get() = L10n.t("be_first_to_rate")

    val retry: String
        get() = L10n.t("retry")

    val browseByCategory: String
        get() = L10n.t("browse_by_category")

    val found: String
        get() = L10n.t("found")

    val videos: String
        get() = L10n.t("videos")

    val noVideosInCategory: String
        get() = L10n.t("no_videos_in_category")

    val beFirstInCategory: String
        get() = L10n.t("be_first_in_category")

    val share: String
        get() = L10n.t("share")

    val clear: String
        get() = L10n.t("clear")

    val clearText: String
        get() = L10n.t("clear_text")

    val sortMenu: String
        get() = L10n.t("sort_menu")

    val userTokenLabel: String
        get() = L10n.t("user_token_label")

    val statusOn: String
        get() = L10n.t("status_on")

    val statusOff: String
        get() = L10n.t("status_off")

    fun adminStatus(isOn: Boolean): String = L10n.t(
        "admin_status",
        if (isOn) statusOn else statusOff
    )

    val psalmSearchHint: String
        get() = L10n.t("psalm_search_hint")

    val closeSearch: String
        get() = L10n.t("close_search")

    val close: String
        get() = L10n.t("close")

    val error: String
        get() = L10n.t("error")

    val loading: String
        get() = L10n.t("loading")

    val loadingVideos: String
        get() = L10n.t("loading_videos")

    val noVideos: String
        get() = L10n.t("no_videos")

    val language: String
        get() = L10n.t("language")

    val contentLanguages: String
        get() = L10n.t("content_languages")

    val selectContentLanguages: String
        get() = L10n.t("select_content_languages")

    val apply: String
        get() = L10n.t("apply")

    val english: String
        get() = L10n.t("english")

    val croatian: String
        get() = L10n.t("croatian")

    val german: String
        get() = L10n.t("german")

    val searchVideo: String
        get() = L10n.t("search_video")

    val clearSearch: String
        get() = L10n.t("clear_search")

    val category: String
        get() = L10n.t("category")

    val reset: String
        get() = L10n.t("reset")

    val previous: String
        get() = L10n.t("previous")

    val next: String
        get() = L10n.t("next")

    val selectChapterToRead: String
        get() = L10n.t("select_chapter_to_read")

    val oldTestament: String
        get() = L10n.t("old_testament")

    val newTestament: String
        get() = L10n.t("new_testament")

    val fontSize: String
        get() = L10n.t("font_size")

    val bibleSearchTitle: String
        get() = L10n.t("bible_search_title")

    val bibleSearchPlaceholder: String
        get() = L10n.t("bible_search_placeholder")

    val startMuted: String
        get() = L10n.t("start_muted")

    val noSavedClips: String
        get() = L10n.t("no_saved_clips")

    val videoThumbnail: String
        get() = L10n.t("video_thumbnail")

    val openInYoutube: String
        get() = L10n.t("open_in_youtube")

    val ratings: String
        get() = L10n.t("ratings")

    val results: String
        get() = L10n.t("results")

    val offlineVideos: String
        get() = L10n.t("offline_videos")

    val addVideo: String
        get() = L10n.t("add_video")

    val noOfflineVideos: String
        get() = L10n.t("no_offline_videos")

    val addVideosToWatchOffline: String
        get() = L10n.t("add_videos_to_watch_offline")

    val addFirstVideo: String
        get() = L10n.t("add_first_video")

    val chooseFromGallery: String
        get() = L10n.t("choose_from_gallery")

    val pickVideoFromDevice: String
        get() = L10n.t("pick_video_from_device")

    val browseDeviceVideos: String
        get() = L10n.t("browse_device_videos")

    val viewAllVideosOnDevice: String
        get() = L10n.t("view_all_videos_on_device")

    val deleteVideo: String
        get() = L10n.t("delete_video")

    fun deleteVideoConfirmation(title: String): String = L10n.t("delete_video_confirmation", title)

    val delete: String
        get() = L10n.t("delete")

    val editVideo: String
        get() = L10n.t("edit_video")

    val videoTitle: String
        get() = L10n.t("video_title")

    val save: String
        get() = L10n.t("save")

    val localVideo: String
        get() = L10n.t("local_video")

    val edit: String
        get() = L10n.t("edit")

    val deviceVideos: String
        get() = L10n.t("device_videos")

    val searchVideos: String
        get() = L10n.t("search_videos")

    val noVideosFound: String
        get() = L10n.t("no_videos_found")

    val noMatchingVideos: String
        get() = L10n.t("no_matching_videos")

    val offline: String
        get() = L10n.t("offline")

    val downloadForOffline: String
        get() = L10n.t("download_for_offline")

    val alreadySavedOffline: String
        get() = L10n.t("already_saved_offline")

    val watchOffline: String
        get() = L10n.t("watch_offline")

    val videoAccessPermission: String
        get() = L10n.t("video_access_permission")

    val videoAccessPermissionExplanation: String
        get() = L10n.t("video_access_permission_explanation")

    val videoAccessPermissionReasons: String
        get() = L10n.t("video_access_permission_reasons")

    val permissionSearchVideos: String
        get() = L10n.t("permission_search_videos")

    val permissionViewMetadata: String
        get() = L10n.t("permission_view_metadata")

    val permissionAccessOldVideos: String
        get() = L10n.t("permission_access_old_videos")

    val permissionManageLibrary: String
        get() = L10n.t("permission_manage_library")

    val videoAccessPrivacy: String
        get() = L10n.t("video_access_privacy")

    val grantPermission: String
        get() = L10n.t("grant_permission")

    val back: String
        get() = L10n.t("back")

    val refresh: String
        get() = L10n.t("refresh")

    val confirm: String
        get() = L10n.t("confirm")

    val dismiss: String
        get() = L10n.t("dismiss")

    val send: String
        get() = L10n.t("send")

    val publish: String
        get() = L10n.t("publish")

    val add: String
        get() = L10n.t("add")

    val open: String
        get() = L10n.t("open")

    val anonymous: String
        get() = L10n.t("anonymous")

    val unknownError: String
        get() = L10n.t("unknown_error")

    val noData: String
        get() = L10n.t("no_data")

    val statistics: String
        get() = L10n.t("statistics")

    val yesterday: String
        get() = L10n.t("yesterday")

    val tomorrow: String
        get() = L10n.t("tomorrow")

    val active: String
        get() = L10n.t("active")

    val noColor: String
        get() = L10n.t("no_color")

    val yesSure: String
        get() = L10n.t("yes_sure")

    val areYouSure: String
        get() = L10n.t("are_you_sure")

    val offlineNoInternet: String
        get() = L10n.t("offline_no_internet")

    val loadingError: String
        get() = L10n.t("loading_error")

    val tryAgain: String
        get() = L10n.t("try_again")

    val settingsTitle: String
        get() = L10n.t("settings_title")

    val checkUpdate: String
        get() = L10n.t("check_update")

    val sectionDisplay: String
        get() = L10n.t("section_display")

    val appBrightness: String
        get() = L10n.t("app_brightness")

    val darkMode: String
        get() = L10n.t("dark_mode")

    val darkModeOn: String
        get() = L10n.t("dark_mode_on")

    val darkModeOff: String
        get() = L10n.t("dark_mode_off")

    val darkModeSystem: String
        get() = L10n.t("dark_mode_system")

    val sectionPersonalization: String
        get() = L10n.t("section_personalization")

    val appColors: String
        get() = L10n.t("app_colors")

    val quickColorSelection: String
        get() = L10n.t("quick_color_selection")

    val resetToDefault: String
        get() = L10n.t("reset_to_default")

    val homeScreenStyle: String
        get() = L10n.t("home_screen_style")

    val styleDefault: String
        get() = L10n.t("style_default")

    val styleCompact: String
        get() = L10n.t("style_compact")

    val styleGallery: String
        get() = L10n.t("style_gallery")

    val sectionLanguageContent: String
        get() = L10n.t("section_language_content")

    val appLanguage: String
        get() = L10n.t("app_language")

    val contentLanguagesLabel: String
        get() = L10n.t("content_languages_label")

    fun selectedCount(count: Int): String = L10n.t("selected_count", count)

    val sectionDataManagement: String
        get() = L10n.t("section_data_management")

    val backupToGoogleDrive: String
        get() = L10n.t("backup_to_google_drive")

    val backupToGoogleDriveHelp: String
        get() = L10n.t("backup_to_google_drive_help")

    val restoreFromGoogleDrive: String
        get() = L10n.t("restore_from_google_drive")

    val restoreFromGoogleDriveHelp: String
        get() = L10n.t("restore_from_google_drive_help")

    val autoGoogleDriveBackup: String
        get() = L10n.t("auto_google_drive_backup")

    val autoBackupEnabled: String
        get() = L10n.t("auto_backup_enabled")

    val autoBackupNoFile: String
        get() = L10n.t("auto_backup_no_file")

    val autoBackupDisabled: String
        get() = L10n.t("auto_backup_disabled")

    val autoBackupToastEnabled: String
        get() = L10n.t("auto_backup_toast_enabled")

    val backupCopy: String
        get() = L10n.t("backup_copy")

    val backupCopyHelp: String
        get() = L10n.t("backup_copy_help")

    val clearCache: String
        get() = L10n.t("clear_cache")

    val cacheOccupied: String
        get() = L10n.t("cache_occupied")

    val sectionAbout: String
        get() = L10n.t("section_about")

    val reportBug: String
        get() = L10n.t("report_bug")

    val reportBugHelp: String
        get() = L10n.t("report_bug_help")

    val privacyPolicy: String
        get() = L10n.t("privacy_policy")

    val privacyPolicyHelp: String
        get() = L10n.t("privacy_policy_help")

    val aboutApp: String
        get() = L10n.t("about_app")

    val version: String
        get() = L10n.t("version")

    val appUsageTime: String
        get() = L10n.t("app_usage_time")

    val notUsedYet: String
        get() = L10n.t("not_used_yet")

    val appName: String
        get() = L10n.t("app_name")

    val appDescription: String
        get() = L10n.t("app_description")

    val adminActivated: String
        get() = L10n.t("admin_activated")

    val adminDeactivated: String
        get() = L10n.t("admin_deactivated")

    val clearCacheQuestion: String
        get() = L10n.t("clear_cache_question")

    fun clearCacheMessage(size: String): String = L10n.t("clear_cache_message", size)

    val clearButton: String
        get() = L10n.t("clear_button")

    val importBackupTitle: String
        get() = L10n.t("import_backup_title")

    val importBackupMessage: String
        get() = L10n.t("import_backup_message")

    val yesImport: String
        get() = L10n.t("yes_import")

    val restoreConfirmTitle: String
        get() = L10n.t("restore_confirm_title")

    val restoreConfirmMessage: String
        get() = L10n.t("restore_confirm_message")

    val lastCopy: String
        get() = L10n.t("last_copy")

    val yesLoad: String
        get() = L10n.t("yes_load")

    val backupTitle: String
        get() = L10n.t("backup_title")

    val lastAutoBackup: String
        get() = L10n.t("last_auto_backup")

    val loadLatestData: String
        get() = L10n.t("load_latest_data")

    val exportBackup: String
        get() = L10n.t("export_backup")

    val exportBackupChooser: String
        get() = L10n.t("export_backup_chooser")

    val backupReadyToShare: String
        get() = L10n.t("backup_ready_to_share")

    val noBackupToExport: String
        get() = L10n.t("no_backup_to_export")

    val noBackupsAvailable: String
        get() = L10n.t("no_backups_available")

    val autoBackupsInfo: String
        get() = L10n.t("auto_backups_info")

    val importBackupFromFile: String
        get() = L10n.t("import_backup_from_file")

    val backupsInfoFooter: String
        get() = L10n.t("backups_info_footer")

    val contentLanguagesTitle: String
        get() = L10n.t("content_languages_title")

    val selectVideoLanguages: String
        get() = L10n.t("select_video_languages")

    val languageDialogTitle: String
        get() = L10n.t("language_dialog_title")

    val bugReportIntro: String
        get() = L10n.t("bug_report_intro")

    val titleLabel: String
        get() = L10n.t("title_label")

    val titlePlaceholder: String
        get() = L10n.t("title_placeholder")

    val descriptionLabel: String
        get() = L10n.t("description_label")

    val descriptionPlaceholder: String
        get() = L10n.t("description_placeholder")

    val emailOptional: String
        get() = L10n.t("email_optional")

    val forFeedback: String
        get() = L10n.t("for_feedback")

    val priorityLabel: String
        get() = L10n.t("priority_label")

    val priorityLow: String
        get() = L10n.t("priority_low")

    val priorityMedium: String
        get() = L10n.t("priority_medium")

    val priorityHigh: String
        get() = L10n.t("priority_high")

    val priorityCritical: String
        get() = L10n.t("priority_critical")

    val bugReportFooter: String
        get() = L10n.t("bug_report_footer")

    val analyticsTitle: String
        get() = L10n.t("analytics_title")

    val analyticsInfoText: String
        get() = L10n.t("analytics_info_text")

    val periodWeek: String
        get() = L10n.t("period_week")

    val periodMonth: String
        get() = L10n.t("period_month")

    val periodYear: String
        get() = L10n.t("period_year")

    val tokenUnavailable: String
        get() = L10n.t("token_unavailable")

    fun errorLoading(msg: String): String = L10n.t("error_loading", msg)

    val noUsageData: String
        get() = L10n.t("no_usage_data")

    val dataWillAppear: String
        get() = L10n.t("data_will_appear")

    val usageSummary: String
        get() = L10n.t("usage_summary")

    val totalTime: String
        get() = L10n.t("total_time")

    val sessions: String
        get() = L10n.t("sessions")

    val averageDaily: String
        get() = L10n.t("average_daily")

    fun averageDailySessions(count: Double): String = L10n.t("average_daily_sessions", count)

    fun retentionRateText(rate: String): String = L10n.t("retention_rate_text", rate)

    val detailedAnalytics: String
        get() = L10n.t("detailed_analytics")

    val dailyStats: String
        get() = L10n.t("daily_stats")

    val time: String
        get() = L10n.t("time")

    val average: String
        get() = L10n.t("average")

    val timeByTabs: String
        get() = L10n.t("time_by_tabs")

    fun visitsAndAverage(count: Int, avgTime: String): String = L10n.t("visits_and_average", count, avgTime)

    val prayersTitle: String
        get() = L10n.t("prayers_title")

    val prayerSubtitle: String
        get() = L10n.t("prayer_subtitle")

    val addNeed: String
        get() = L10n.t("add_need")

    val deletePrayer: String
        get() = L10n.t("delete_prayer")

    val deletePrayerWarning: String
        get() = L10n.t("delete_prayer_warning")

    val didIPray: String
        get() = L10n.t("did_ipray")

    val confirmPrayedMessage: String
        get() = L10n.t("confirm_prayed_message")

    val areYouReallySure: String
        get() = L10n.t("are_you_really_sure")

    val confirmPrayedAgain: String
        get() = L10n.t("confirm_prayed_again")

    val iPrayedChecked: String
        get() = L10n.t("i_prayed_checked")

    val iPrayed: String
        get() = L10n.t("i_prayed")

    val encourage: String
        get() = L10n.t("encourage")

    val details: String
        get() = L10n.t("details")

    val needLabel: String
        get() = L10n.t("need_label")

    val encouragements: String
        get() = L10n.t("encouragements")

    val noEncouragements: String
        get() = L10n.t("no_encouragements")

    val newPrayerNeed: String
        get() = L10n.t("new_prayer_need")

    val writeNeed: String
        get() = L10n.t("write_need")

    val needPlaceholder: String
        get() = L10n.t("need_placeholder")

    val nameOptional: String
        get() = L10n.t("name_optional")

    val minChars5: String
        get() = L10n.t("min_chars5")

    val encouragementTitle: String
        get() = L10n.t("encouragement_title")

    val writeMessage: String
        get() = L10n.t("write_message")

    val messagePlaceholder: String
        get() = L10n.t("message_placeholder")

    val minChars2: String
        get() = L10n.t("min_chars2")

    val noPrayerNeeds: String
        get() = L10n.t("no_prayer_needs")

    val addNeedCommunity: String
        get() = L10n.t("add_need_community")

    val bibleTitle: String
        get() = L10n.t("bible_title")

    val resetTodayQuestion: String
        get() = L10n.t("reset_toquestion")

    val resetTodayMessage: String
        get() = L10n.t("reset_tomessage")

    val resetToday: String
        get() = L10n.t("reset_today")

    val tabToday: String
        get() = L10n.t("tab_today")

    val tabSettings: String
        get() = L10n.t("tab_settings")

    val dailyProgress: String
        get() = L10n.t("daily_progress")

    fun dailyProgressSubtitle(units: Int, goal: Int, points: Int, streak: Int): String = L10n.t("daily_progress_subtitle", units, goal, points, streak)

    val dailyPsalmsTitle: String
        get() = L10n.t("daily_psalms_title")

    fun dailyPsalmsSubtitle(done: Int, total: Int): String = L10n.t("daily_psalms_subtitle", done, total)

    fun psalmNumber(number: Int): String = L10n.t("psalm_number", number)

    val bibleRule: String
        get() = L10n.t("bible_rule")

    val reading: String
        get() = L10n.t("reading")

    val thisWeek: String
        get() = L10n.t("this_week")

    val thisMonth: String
        get() = L10n.t("this_month")

    val thisYear: String
        get() = L10n.t("this_year")

    fun todayCount(count: Int): String = L10n.t("tocount", count)

    val meditationTitle: String
        get() = L10n.t("meditation_title")

    fun meditationSubtitle(min: Int): String = L10n.t("meditation_subtitle", min)

    val sequentialReading: String
        get() = L10n.t("sequential_reading")

    fun sequentialSubtitle(name: String, chapter: Int, total: Int, percent: Int): String = L10n.t("sequential_subtitle", name, chapter, total, percent)

    val noSelection: String
        get() = L10n.t("no_selection")

    val fromBeginning: String
        get() = L10n.t("from_beginning")

    val readerInfo: String
        get() = L10n.t("reader_info")

    val dailyGoal: String
        get() = L10n.t("daily_goal")

    val goalInfo: String
        get() = L10n.t("goal_info")

    val saveGoal: String
        get() = L10n.t("save_goal")

    val fastingTitle: String
        get() = L10n.t("fasting_title")

    val fastingSubtitle: String
        get() = L10n.t("fasting_subtitle")

    val fastingDetails: String
        get() = L10n.t("fasting_details")

    val previousMonth: String
        get() = L10n.t("previous_month")

    val nextMonth: String
        get() = L10n.t("next_month")

    val currentStreak: String
        get() = L10n.t("current_streak")

    val longestStreak: String
        get() = L10n.t("longest_streak")

    val dayMon: String
        get() = L10n.t("mon")

    val dayTue: String
        get() = L10n.t("tue")

    val dayWed: String
        get() = L10n.t("wed")

    val dayThu: String
        get() = L10n.t("thu")

    val dayFri: String
        get() = L10n.t("fri")

    val daySat: String
        get() = L10n.t("sat")

    val daySun: String
        get() = L10n.t("sun")

    val fastingActive: String
        get() = L10n.t("fasting_active")

    val fastingInactive: String
        get() = L10n.t("fasting_inactive")

    val fastingWater: String
        get() = L10n.t("fasting_water")

    val fastingBread: String
        get() = L10n.t("fasting_bread")

    val fastingFull: String
        get() = L10n.t("fasting_full")

    fun durationLabel(hours: Int): String = L10n.t("duration_label", hours)

    val noteLabel: String
        get() = L10n.t("note_label")

    val openDetails: String
        get() = L10n.t("open_details")

    val total: String
        get() = L10n.t("total")

    val recentFasts: String
        get() = L10n.t("recent_fasts")

    val weeklyProgress: String
        get() = L10n.t("weekly_progress")

    fun daysOfFasting(completed: Int, goal: Int): String = L10n.t("days_of_fasting", completed, goal)

    fun goalPercentage(percent: Int): String = L10n.t("goal_percentage", percent)

    val weeklyGoalTitle: String
        get() = L10n.t("weekly_goal_title")

    fun daysOfFastingGoal(days: Int): String = L10n.t("days_of_fasting_goal", days)

    val reminderTitle: String
        get() = L10n.t("reminder_title")

    val dailyFastingReminder: String
        get() = L10n.t("daily_fasting_reminder")

    val disabled: String
        get() = L10n.t("disabled")

    val gospelTitle: String
        get() = L10n.t("gospel_title")

    val gospelSectionGospel: String
        get() = L10n.t("gospel_section_gospel")

    val gospelFirstReading: String
        get() = L10n.t("gospel_first_reading")

    val gospelPsalm: String
        get() = L10n.t("gospel_psalm")

    fun dateLabel(date: String): String = L10n.t("date_label", date)

    val rosaryTitle: String
        get() = L10n.t("rosary_title")

    val rosaryHeader: String
        get() = L10n.t("rosary_header")

    val todaysMysteries: String
        get() = L10n.t("todays_mysteries")

    val startRosary: String
        get() = L10n.t("start_rosary")

    val tapForNextPrayer: String
        get() = L10n.t("tap_for_next_prayer")

    val signOfCross: String
        get() = L10n.t("sign_of_cross")

    val apostlesCreed: String
        get() = L10n.t("apostles_creed")

    val ourFather: String
        get() = L10n.t("our_father")

    fun hailMaryIntro(n: Int): String = L10n.t("hail_mary_intro", n)

    val gloryBe: String
        get() = L10n.t("glory_be")

    fun mysteryLabel(n: Int): String = L10n.t("mystery_label", n)

    fun hailMaryDecade(n: Int): String = L10n.t("hail_mary_decade", n)

    val fatimaPrayer: String
        get() = L10n.t("fatima_prayer")

    val hailHolyQueen: String
        get() = L10n.t("hail_holy_queen")

    val rosaryFinished: String
        get() = L10n.t("rosary_finished")

    val rosaryStreakLabel: String
        get() = L10n.t("rosary_streak_label")

    fun rosaryDaysValue(days: Int): String = L10n.t("rosary_days_value", days)

    val rosaryTotalLabel: String
        get() = L10n.t("rosary_total_label")

    val rosarySessionsLabel: String
        get() = L10n.t("rosary_sessions_label")

    fun progressLabel(current: Int, total: Int): String = L10n.t("progress_label", current, total)

    val ratingQuestion: String
        get() = L10n.t("rating_question")

    val loveLabelEmoji: String
        get() = L10n.t("love_label_emoji")

    val faithLabelEmoji: String
        get() = L10n.t("faith_label_emoji")

    val hopeLabelEmoji: String
        get() = L10n.t("hope_label_emoji")

    val categoryOptional: String
        get() = L10n.t("category_optional")

    val selectLanguage: String
        get() = L10n.t("select_language")

    val languagePrefix: String
        get() = L10n.t("language_prefix")

    fun selectMissing(missing: String): String = L10n.t("select_missing", missing)

    val rateButton: String
        get() = L10n.t("rate_button")

    val enterAllRatings: String
        get() = L10n.t("enter_all_ratings")

    val rateOncePerVideo: String
        get() = L10n.t("rate_once_per_video")

    val deleteConfirmTitle: String
        get() = L10n.t("delete_confirm_title")

    val cancelAction: String
        get() = L10n.t("cancel_action")

    val widgetNoFavorites: String
        get() = L10n.t("widget_no_favorites")

    val widgetAddFavoriteHint: String
        get() = L10n.t("widget_add_favorite_hint")

    val noInternetConnection: String
        get() = L10n.t("no_internet_connection")

    val timeoutTryAgain: String
        get() = L10n.t("timeout_try_again")

    val videoInfoIncomplete: String
        get() = L10n.t("video_info_incomplete")

    val ratingsMustBe1To3: String
        get() = L10n.t("ratings_must_be1_to3")

    val missingVideoData: String
        get() = L10n.t("missing_video_data")

    val canRateOncePerVideo: String
        get() = L10n.t("can_rate_once_per_video")

    val userTokenNotSet: String
        get() = L10n.t("user_token_not_set")

    val errorSendingRating: String
        get() = L10n.t("error_sending_rating")

    fun errorLoadingVideo(message: String?): String = L10n.t("error_loading_video", message)

    fun genericError(message: String?): String = L10n.t("generic_error", message)

    val errorLoadingGeneric: String
        get() = L10n.t("error_loading_generic")

    val errorAdding: String
        get() = L10n.t("error_adding")

    val errorSendingEncouragement: String
        get() = L10n.t("error_sending_encouragement")

    val deletionFailed: String
        get() = L10n.t("deletion_failed")

    val noInternetCheckNetwork: String
        get() = L10n.t("no_internet_check_network")

    val errorLoadingVideos: String
        get() = L10n.t("error_loading_videos")

    val networkError: String
        get() = L10n.t("network_error")

    val myNotesTitle: String
        get() = L10n.t("my_notes_title")

    fun notesCount(count: Int): String = L10n.t("notes_count", count)

    val noNotes: String
        get() = L10n.t("no_notes")

    val addFirstNote: String
        get() = L10n.t("add_first_note")

    val newNote: String
        get() = L10n.t("new_note")

    val editNote: String
        get() = L10n.t("edit_note")

    val deleteNoteTitle: String
        get() = L10n.t("delete_note_title")

    fun deleteNoteWarning(title: String): String = L10n.t("delete_note_warning", title)

    val contentLabel: String
        get() = L10n.t("content_label")

    val noteContentPlaceholder: String
        get() = L10n.t("note_content_placeholder")

    val failedLoadNotes: String
        get() = L10n.t("failed_load_notes")

    fun failedAddNote(message: String?): String = L10n.t("failed_add_note", message)

    fun failedUpdateNote(message: String?): String = L10n.t("failed_update_note", message)

    fun failedDeleteNote(message: String?): String = L10n.t("failed_delete_note", message)

    fun failedGetNote(message: String?): String = L10n.t("failed_get_note", message)

    val saintsTitle: String
        get() = L10n.t("saints_title")

    val newSaint: String
        get() = L10n.t("new_saint")

    val editSaint: String
        get() = L10n.t("edit_saint")

    val nameLabel: String
        get() = L10n.t("name_label")

    val feastDay: String
        get() = L10n.t("feast_day")

    val patronsLabel: String
        get() = L10n.t("patrons_label")

    val lifeStory: String
        get() = L10n.t("life_story")

    val patronLabel: String
        get() = L10n.t("patron_label")

    fun feastDayLabel(day: String): String = L10n.t("feast_label", day)

    val backToList: String
        get() = L10n.t("back_to_list")

    val galleryTitle: String
        get() = L10n.t("gallery_title")

    val addImagesTitle: String
        get() = L10n.t("add_images_title")

    val selectFromGallery: String
        get() = L10n.t("select_from_gallery")

    val multipleImagesInfo: String
        get() = L10n.t("multiple_images_info")

    val refreshFromServer: String
        get() = L10n.t("refresh_from_server")

    val fetchLatestImagesInfo: String
        get() = L10n.t("fetch_latest_images_info")

    val deleteImage: String
        get() = L10n.t("delete_image")

    val deleteImageWarning: String
        get() = L10n.t("delete_image_warning")

    fun galleryStats(total: Int, server: Int, local: Int, pinned: Int): String = L10n.t("gallery_stats", total, server, local, pinned)

    fun lastSyncLabel(time: String): String = L10n.t("last_sync_label", time)

    val refreshServer: String
        get() = L10n.t("refresh_server")

    val noImagesInGallery: String
        get() = L10n.t("no_images_in_gallery")

    val addImagesToSeeHere: String
        get() = L10n.t("add_images_to_see_here")

    val pin: String
        get() = L10n.t("pin")

    val serverLabel: String
        get() = L10n.t("server_label")

    val imageEditorTitle: String
        get() = L10n.t("image_editor_title")

    val loadingImage: String
        get() = L10n.t("loading_image")

    val imageNotAvailable: String
        get() = L10n.t("image_not_available")

    val closeEditor: String
        get() = L10n.t("close_editor")

    val saveImage: String
        get() = L10n.t("save_image")

    val markerSize: String
        get() = L10n.t("marker_size")

    val clearAnnotations: String
        get() = L10n.t("clear_annotations")

    val textLabel: String
        get() = L10n.t("text_label")

    val placeText: String
        get() = L10n.t("place_text")

    val tapToPlaceText: String
        get() = L10n.t("tap_to_place_text")

    val undo: String
        get() = L10n.t("undo")

    val redo: String
        get() = L10n.t("redo")

    fun imagesAddedSummary(added: Int, failed: Int): String = L10n.t("images_added_summary", added, failed)

    val imageSavedSuccess: String
        get() = L10n.t("image_saved_success")

    val imageSaveFailed: String
        get() = L10n.t("image_save_failed")

    val bibleStatsTitle: String
        get() = L10n.t("bible_stats_title")

    val bibleTotalReadings: String
        get() = L10n.t("bible_total_readings")

    val bibleMeditation: String
        get() = L10n.t("bible_meditation")

    val bibleLongestStreak: String
        get() = L10n.t("bible_longest_streak")

    val bibleActiveDays: String
        get() = L10n.t("bible_active_days")

    val last7Days: String
        get() = L10n.t("last7_days")

    val last30Days: String
        get() = L10n.t("last30_days")

    fun pointsLabel(points: Int): String = L10n.t("points_label", points)

    val bibleValueLabel: String
        get() = L10n.t("bible_value_label")

    val meditationMinLabel: String
        get() = L10n.t("meditation_min_label")

    val prayersLabel: String
        get() = L10n.t("prayers_label")

    val forOthers: String
        get() = L10n.t("for_others")

    val searchPrayers: String
        get() = L10n.t("search_prayers")

    val showAll: String
        get() = L10n.t("show_all")

    val rosaryLabel: String
        get() = L10n.t("rosary_label")

    val encouragementLabel: String
        get() = L10n.t("encouragement_label")

    val averageLabel: String
        get() = L10n.t("average_label")

    val myRatingsTitle: String
        get() = L10n.t("my_ratings_title")

    val ratedVideosEmptyMessage: String
        get() = L10n.t("rated_videos_empty_message")

    val deleteRating: String
        get() = L10n.t("delete_rating")

    val deleteRatingWarning: String
        get() = L10n.t("delete_rating_warning")

    val editRating: String
        get() = L10n.t("edit_rating")

    val missingActivityContext: String
        get() = L10n.t("missing_activity_context")

    val continueWatchingTitle: String
        get() = L10n.t("continue_watching_title")

    val continueWatchingText: String
        get() = L10n.t("continue_watching_text")

    val continueHere: String
        get() = L10n.t("continue_here")

    val openInBrowser: String
        get() = L10n.t("open_in_browser")

    val openInYouTube: String
        get() = L10n.t("open_in_youtube")

    val clips: String
        get() = L10n.t("clips")

    val languageNotRecognized: String
        get() = L10n.t("language_not_recognized")

    val miniPlayerBottom: String
        get() = L10n.t("mini_player_bottom")

    val retryAgain: String
        get() = L10n.t("retry_again")

    val gospelSection: String
        get() = L10n.t("gospel_section")

    val firstReadingSection: String
        get() = L10n.t("first_reading_section")

    val psalmSection: String
        get() = L10n.t("psalm_section")

    val errorLoadingDot: String
        get() = L10n.t("error_loading_dot")

    val errorDot: String
        get() = L10n.t("error_dot")

    val quickPsalms: String
        get() = L10n.t("quick_psalms")

    val psalms: String
        get() = L10n.t("psalms")

    fun chapterLabel(chapter: String): String = L10n.t("chapter_label", chapter)

    fun hailMaryCount(number: Int, total: Int): String = L10n.t("hail_mary_count", number, total)

    fun decadeMystery(decade: Int): String = L10n.t("decade_mystery", decade)

    val minimumCharsRequired5: String
        get() = L10n.t("minimum_chars_required5")

    val minimumCharsRequired2: String
        get() = L10n.t("minimum_chars_required2")

    val encouragementPlaceholder: String
        get() = L10n.t("encouragement_placeholder")

    val addNeedAndCommunityWillPray: String
        get() = L10n.t("add_need_and_community_will_pray")

    val display: String
        get() = L10n.t("display")

    val gridView: String
        get() = L10n.t("grid_view")

    val listViewLabel: String
        get() = L10n.t("list_view_label")

    fun videoCountLabel(count: Int): String = L10n.t("video_count_label", count)

    val sorting: String
        get() = L10n.t("sorting")

    val order: String
        get() = L10n.t("order")

    val sortNewest: String
        get() = L10n.t("sort_newest")

    val sortBestRated: String
        get() = L10n.t("sort_best_rated")

    val sortBestScore: String
        get() = L10n.t("sort_best_score")

    val reportVideo: String
        get() = L10n.t("report_video")

    val noTitle: String
        get() = L10n.t("no_title")

    val addToFavorites: String
        get() = L10n.t("add_to_favorites")

    val videoLoadError: String
        get() = L10n.t("video_load_error")

    fun httpErrorLoading(code: Int): String = L10n.t("http_error_loading", code)

    val fileNotFound: String
        get() = L10n.t("file_not_found")

    val videoNotAvailableOnDevice: String
        get() = L10n.t("video_not_available_on_device")

    val failedLoadVideos: String
        get() = L10n.t("failed_load_videos")

    fun failedScanDeviceVideos(message: String?): String = L10n.t("failed_scan_device_videos", message)

    val failedAddVideo: String
        get() = L10n.t("failed_add_video")

    fun failedAddVideoDetail(message: String?): String = L10n.t("failed_add_video_detail", message)

    val failedDeleteVideo: String
        get() = L10n.t("failed_delete_video")

    fun failedDeleteVideoDetail(message: String?): String = L10n.t("failed_delete_video_detail", message)

    val failedUpdateVideo: String
        get() = L10n.t("failed_update_video")

    fun failedUpdateVideoDetail(message: String?): String = L10n.t("failed_update_video_detail", message)

    fun backupCreatedSuccess(fileName: String): String = L10n.t("backup_created_success", fileName)

    val successfullyRestored: String
        get() = L10n.t("successfully_restored")

    val successfullyImported: String
        get() = L10n.t("successfully_imported")

    val errorDuringImport: String
        get() = L10n.t("error_during_import")

    fun errorDuringImportDetail(message: String?): String = L10n.t("error_during_import_detail", message)

    val backupSavedToGoogleDrive: String
        get() = L10n.t("backup_saved_to_google_drive")

    fun errorSavingToGoogleDrive(message: String?): String = L10n.t("error_saving_to_google_drive", message)

    val cannotReadFile: String
        get() = L10n.t("cannot_read_file")

    val successfullyImportedFromGoogleDrive: String
        get() = L10n.t("successfully_imported_from_google_drive")

    fun errorDuringCreation(message: String?): String = L10n.t("error_during_creation", message)

    fun errorDuringRestore(message: String?): String = L10n.t("error_during_restore", message)

    val errorRestoringBackup: String
        get() = L10n.t("error_restoring_backup")

    fun errorDuringLoading(message: String?): String = L10n.t("error_during_loading", message)

    fun monthTotalDays(days: Int): String = L10n.t("month_total_days", days)

    val crashTestWarning: String
        get() = L10n.t("crash_test_warning")

    val continueQuestion: String
        get() = L10n.t("continue_question")

    val password: String
        get() = L10n.t("password")

    val importBackupWarning: String
        get() = L10n.t("import_backup_warning")

    val loadConfirmTitle: String
        get() = L10n.t("load_confirm_title")

    val loadBackupWarning: String
        get() = L10n.t("load_backup_warning")

    val backupCopyTitle: String
        get() = L10n.t("backup_copy_title")

    val appLanguageTitle: String
        get() = L10n.t("app_language_title")

    val feedbackTitleLabel: String
        get() = L10n.t("feedback_title_label")

    val feedbackTitlePlaceholder: String
        get() = L10n.t("feedback_title_placeholder")

    val feedbackDetailLabel: String
        get() = L10n.t("feedback_detail_label")

    val feedbackDetailPlaceholder: String
        get() = L10n.t("feedback_detail_placeholder")

    val feedbackEmailLabel: String
        get() = L10n.t("feedback_email_label")

    val feedbackEmailPlaceholder: String
        get() = L10n.t("feedback_email_placeholder")

    val selectBookForOffline: String
        get() = L10n.t("select_book_for_offline")

    val applying: String
        get() = L10n.t("applying")

    val deleteVideoConfirmTitle: String
        get() = L10n.t("delete_video_confirm_title")

    val deleteVideoConfirmText: String
        get() = L10n.t("delete_video_confirm_text")

    val enterLink: String
        get() = L10n.t("enter_link")

    val enterLanguageHint: String
        get() = L10n.t("enter_language_hint")

    val reportVideoQuestion: String
        get() = L10n.t("report_video_question")

    val reportVideoConfirmText: String
        get() = L10n.t("report_video_confirm_text")

    val enterYoutubeUrl: String
        get() = L10n.t("enter_youtube_url")

    val report: String
        get() = L10n.t("report")

    val resetView: String
        get() = L10n.t("reset_view")

    val rate: String
        get() = L10n.t("rate")

    val rateVideo: String
        get() = L10n.t("rate_video")

    val startTimeLabel: String
        get() = L10n.t("start_time_label")

    val endTimeLabel: String
        get() = L10n.t("end_time_label")

    val saveClip: String
        get() = L10n.t("save_clip")

    val watchHistoryTitle: String
        get() = L10n.t("watch_history_title")

    val deleteHistoryTitle: String
        get() = L10n.t("delete_history_title")

    val deleteHistoryWarning: String
        get() = L10n.t("delete_history_warning")

    val newest: String
        get() = L10n.t("newest")

    val oldest: String
        get() = L10n.t("oldest")

    val bestRated: String
        get() = L10n.t("best_rated")

    val groups: String
        get() = L10n.t("groups")

    val singleList: String
        get() = L10n.t("single_list")

    val deleteAllSaved: String
        get() = L10n.t("delete_all_saved")

    val openGallery: String
        get() = L10n.t("open_gallery")

    val ok: String
        get() = L10n.t("ok")

    fun galleryCountLabel(count: Int): String = L10n.t("gallery_count_label", count)

    val ratingTypeHope: String
        get() = L10n.t("rating_type_hope")

    val ratingTypeLove: String
        get() = L10n.t("rating_type_love")

    val ratingTypeFaith: String
        get() = L10n.t("rating_type_faith")

    val allText: String
        get() = L10n.t("all_text")

    val toTop: String
        get() = L10n.t("to_top")

    val unsupportedLink: String
        get() = L10n.t("unsupported_link")

    val ratingSuccessfully: String
        get() = L10n.t("rating_successfully")

    val selectVideos: String
        get() = L10n.t("select_videos")

    val offlineNoCon: String
        get() = L10n.t("offline_no_con")

    val pullRefreshOrAdd: String
        get() = L10n.t("pull_refresh_or_add")

    val videosFound: String
        get() = L10n.t("videos_found")

    val noVideosDisplay: String
        get() = L10n.t("no_videos_display")

    val deleteConfirmation: String
        get() = L10n.t("delete_confirmation")

    val waitForVideoLoad: String
        get() = L10n.t("wait_for_video_load")

    val alreadyShuffling: String
        get() = L10n.t("already_shuffling")

    val noVideosToShuffle: String
        get() = L10n.t("no_videos_to_shuffle")

    val notEnoughToShuffle: String
        get() = L10n.t("not_enough_to_shuffle")

    val tokenLoading: String
        get() = L10n.t("token_loading")

    val reportSubmission: String
        get() = L10n.t("report_submission")

    val thanksReport: String
        get() = L10n.t("thanks_report")

    val reportError: String
        get() = L10n.t("report_error")

    val deleteSuccess: String
        get() = L10n.t("delete_success")

    val deleteError: String
        get() = L10n.t("delete_error")

    val loadingYoutube: String
        get() = L10n.t("loading_youtube")

    val onlyYoutubeSupported: String
        get() = L10n.t("only_youtube_supported")

    val validateYoutube: String
        get() = L10n.t("validate_youtube")

    val random: String
        get() = L10n.t("random")

    val randomPassageTitle: String
        get() = L10n.t("random_passage_title")

    val paste: String
        get() = L10n.t("paste")

    val validLink: String
        get() = L10n.t("valid_link")

    val unrecognizedLanguage: String
        get() = L10n.t("unrecognized_language")

    val fastRating: String
        get() = L10n.t("fast_rating")

    val quickMenu: String
        get() = L10n.t("quick_menu")

    val favorite: String
        get() = L10n.t("favorite")

    val reporting: String
        get() = L10n.t("reporting")

    val reportNote: String
        get() = L10n.t("report_note")

    val adminDeletedSuccess: String
        get() = L10n.t("admin_deleted_success")

    val adminDeleteError: String
        get() = L10n.t("admin_delete_error")

    val loadError: String
        get() = L10n.t("load_error")

    fun featureName(key: String): String = when (key) {
        "video_rating" -> L10n.t("feature_name_video_rating")
        "prayer" -> L10n.t("feature_name_prayer")
        "gospel" -> L10n.t("feature_name_gospel")
        "bible" -> L10n.t("feature_name_bible")
        "favorites" -> L10n.t("feature_name_favorites")
        else -> key.replace("_", " ")
    }

    fun tabName(key: String): String = when (key) {
        "home" -> L10n.t("tab_name_home")
        "bible" -> L10n.t("tab_name_bible")
        "prayer" -> L10n.t("tab_name_prayer")
        "favorites" -> L10n.t("tab_name_favorites")
        "notes" -> L10n.t("tab_name_notes")
        "analytics" -> L10n.t("tab_name_analytics")
        "settings" -> L10n.t("tab_name_settings")
        "rosary" -> L10n.t("tab_name_rosary")
        "saints" -> L10n.t("tab_name_saints")
        "rated" -> L10n.t("tab_name_rated")
        "fasting" -> L10n.t("tab_name_fasting")
        else -> key.replace("_", " ")
    }

}
