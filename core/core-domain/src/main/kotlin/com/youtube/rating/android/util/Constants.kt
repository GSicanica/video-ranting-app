package com.youtube.rating.android.util

import com.youtube.rating.android.util.VideoConstants.MS_PER_HOUR

/**
 * Application-wide constants
 * Single source of truth for all magic numbers and configuration values
 */
object RatingConstants {
    // Rating System (1-3 hearts per category)
    const val MIN_RATING = 1
    const val MAX_RATING = 3
    const val MIN_RATING_FLOAT = 1f
    const val MAX_RATING_FLOAT = 3f
    
    // Legacy rating system (1-3 hearts) - for compatibility
    const val LEGACY_MIN_RATING = 1f
    const val LEGACY_MAX_RATING = 3f
    
    // Default values
    const val DEFAULT_RATING = 0.0
    const val DEFAULT_RATING_INT = 0
    
    // Categories
    const val CATEGORY_LOVE = "love"
    const val CATEGORY_FAITH = "faith"
    const val CATEGORY_HOPE = "hope"
    
    // Rating thresholds
    const val MIN_LOVE_THRESHOLD = 0
    const val MIN_FAITH_THRESHOLD = 0
    const val MIN_HOPE_THRESHOLD = 0
    
    // Number of rating categories
    const val RATING_CATEGORIES_COUNT = 3
    const val HEARTS_PER_CATEGORY = 3
}

object NoteConstants {
    // Note validation
    const val MAX_TITLE_LENGTH = 100
    const val MAX_CONTENT_LENGTH = 5000
    const val MIN_TITLE_LENGTH = 1
}

object CacheConstants {
    // Cache durations (milliseconds)
    const val CACHE_DURATION_DEFAULT = 5 * 60 * 1000L // 5 minutes
    const val CACHE_DURATION_VIDEO_STATS = 2 * 60 * 1000L // 2 minutes
    const val CACHE_DURATION_SEARCH = 10 * 60 * 1000L // 10 minutes
    const val CACHE_DURATION_USER_RATINGS = 5 * 60 * 1000L // 5 minutes
    const val CACHE_DURATION_TRENDING = 10 * 60 * 1000L // 10 minutes
    
    // Legacy constants (kept for compatibility)
    const val VIDEO_INFO_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes
    const val VIDEO_STATS_CACHE_DURATION = 2 * 60 * 1000L // 2 minutes
    const val SEARCH_RESULTS_CACHE_DURATION = 10 * 60 * 1000L // 10 minutes
    const val FAVORITES_CACHE_DURATION = 30 * 1000L // 30 seconds
    const val VIDEO_CACHE_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes
    
    // Cache sizes
    const val MAX_CACHE_SIZE_MB = 100
    const val MAX_CACHE_SIZE_BYTES = MAX_CACHE_SIZE_MB * 1024 * 1024L
    
    // Memory cache percentage
    const val MEMORY_CACHE_PERCENT = 0.25 // 25% of available memory
    
    // Cache cleanup threshold
    const val CACHE_CLEANUP_SIZE_MB = 80 // Cleanup when reaching 80MB
}

object PaginationConstants {
    // Pagination defaults
    const val DEFAULT_PAGE = 1
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    const val PREFETCH_DISTANCE = 5
    const val INITIAL_LOAD_SIZE = 20
}

object NetworkConstants {
    // Network timeouts (milliseconds)
    const val CONNECT_TIMEOUT_MS = 30000 // 30 seconds
    const val READ_TIMEOUT_MS = 30000 // 30 seconds
    const val WRITE_TIMEOUT_MS = 30000 // 30 seconds
    const val SHORT_TIMEOUT_MS = 5000 // 5 seconds for quick requests
    
    // Retry configuration
    const val MAX_RETRIES = 3
    const val RETRY_DELAY_MS = 1000L
    const val EXPONENTIAL_BACKOFF_MULTIPLIER = 2
}

object VideoConstants {
    // Video URL patterns
    const val YOUTUBE_WATCH_URL = "youtube.com/watch?v="
    const val YOUTUBE_SHORT_URL = "youtu.be/"
    
    // Video ID length
    const val YOUTUBE_VIDEO_ID_LENGTH = 11
    
    // Video duration threshold (milliseconds)
    const val MIN_VIDEO_DURATION_MS = 1000L // 1 second
    
    // Time conversion
    const val SECONDS_PER_MINUTE = 60
    const val MINUTES_PER_HOUR = 60
    const val HOURS_PER_DAY = 24
    const val MS_PER_SECOND = 1000L
    const val MS_PER_MINUTE = 60 * MS_PER_SECOND
    const val MS_PER_HOUR = 60 * MS_PER_MINUTE
    const val MS_PER_DAY = 24 * MS_PER_HOUR
}

object UIConstants {
    // Animation durations (milliseconds)
    const val ANIMATION_DURATION_SHORT = 150
    const val ANIMATION_DURATION_MEDIUM = 300
    const val ANIMATION_DURATION_LONG = 500
    const val ANIMATION_DURATION_EXTRA_LONG = 1000
    
    // Shimmer animation
    const val SHIMMER_TRANSLATION_DISTANCE = 1000f
    const val SHIMMER_OFFSET = 500f
    
    // Debounce delays (milliseconds)
    const val SEARCH_DEBOUNCE_MS = 500L
    const val TYPING_DEBOUNCE_MS = 300L
    const val AUTO_BACKUP_DELAY_MS = 2000L // Wait 2 seconds after last change
    
    // UI thresholds
    const val MIN_RATINGS_TO_DISPLAY = 0
    const val EMPTY_LIST_THRESHOLD = 0
    
    // Alpha values
    const val ALPHA_DISABLED = 0.3f
    const val ALPHA_MEDIUM = 0.5f
    const val ALPHA_ENABLED = 0.6f
    const val ALPHA_FULL = 1.0f
    
    // Width fractions
    const val WIDTH_FRACTION_SMALL = 0.6f
    const val WIDTH_FRACTION_MEDIUM = 0.8f
    const val WIDTH_FRACTION_LARGE = 0.9f
    
    // Icon sizes (dp)
    const val ICON_SIZE_SMALL = 24
    const val ICON_SIZE_MEDIUM = 40
    const val ICON_SIZE_LARGE = 48
    const val ICON_SIZE_EXTRA_LARGE = 120
    
    // Grid columns
    const val GRID_COLUMNS = 2
    
    // Monitoring intervals
    const val FPS_MONITOR_INTERVAL_MS = 1000L
    const val MEMORY_MONITOR_INTERVAL_MS = 1000L
    const val MAX_SNAPSHOTS_HISTORY = 1000

    // YouTube info cache
    const val YT_INFO_CACHE_TTL_MS = 6 * 60 * 60 * 1000L
}

object StorageConstants {
    // File size thresholds
    const val BYTES_PER_KB = 1024
    const val BYTES_PER_MB = 1024 * BYTES_PER_KB
    const val BYTES_PER_GB = 1024 * BYTES_PER_MB
    
    // Size display thresholds
    const val GB_THRESHOLD = 1.0
    const val MB_THRESHOLD = 1.0
    const val KB_THRESHOLD = 1.0
    
    // Backup settings
    const val AUTO_BACKUP_INTERVAL_MS = 24 * VideoConstants.MS_PER_HOUR // 24 hours
}

object HapticConstants {
    // Vibration patterns (milliseconds)
    const val HAPTIC_CLICK_DURATION = 10
    const val HAPTIC_NO_REPEAT = -1
    
    // Success pattern cannot be const (LongArray not allowed)
    val HAPTIC_SUCCESS_PATTERN = longArrayOf(0, 100, 30, 100)
}

object PerformanceConstants {
    // FPS thresholds
    const val TARGET_FPS = 60
    const val MIN_ACCEPTABLE_FPS = 30
    const val FPS_CALCULATION_DIVISOR = 1000f
    
    // Memory thresholds (percentage)
    const val MEMORY_WARNING_THRESHOLD = 80
    const val MEMORY_CRITICAL_THRESHOLD = 90
    const val MEMORY_PERCENT_MULTIPLIER = 100.0
}

object FormatConstants {
    // Number formats
    const val DECIMAL_FORMAT_2 = "%.2f"
    const val DECIMAL_FORMAT_1 = "%.1f"
    const val DECIMAL_FORMAT_0 = "%.0f"
}

object ErrorMessages {
    // Validation errors
    const val ERROR_EMPTY_URL = "URL ne može biti prazan"
    const val ERROR_INVALID_URL = "Nevažeća YouTube URL adresa"
    const val ERROR_INVALID_RATING = "Ocjena mora biti između ${RatingConstants.MIN_RATING} i ${RatingConstants.MAX_RATING}"
    const val ERROR_TITLE_TOO_LONG = "Naslov ne može biti duži od ${NoteConstants.MAX_TITLE_LENGTH} znakova"
    const val ERROR_CONTENT_TOO_LONG = "Sadržaj ne može biti duži od ${NoteConstants.MAX_CONTENT_LENGTH} znakova"
    const val ERROR_TITLE_EMPTY = "Naslov ne može biti prazan"
    
    // Network errors
    const val ERROR_NETWORK_UNAVAILABLE = "Nema internet konekcije"
    const val ERROR_SERVER_UNAVAILABLE = "Server trenutno nije dostupan"
    const val ERROR_TIMEOUT = "Vrijeme čekanja je isteklo"
    
    // Generic errors
    const val ERROR_UNKNOWN = "Došlo je do nepoznate greške"
    const val ERROR_LOADING_FAILED = "Učitavanje nije uspjelo"
}
