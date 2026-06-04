package com.youtube.rating.android.home

import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.ui.models.BrowseRatingFilters
import com.youtube.rating.android.ui.models.HomeScreenUiState
import com.youtube.rating.shared.models.HomeCategory
import com.youtube.rating.shared.models.SaintOfDayResponse
import com.youtube.rating.shared.models.VideoSearchResult
import kotlinx.serialization.Serializable

internal const val MIN_RATING = 1f
internal const val MAX_RATING = 3f

internal const val SEARCH_TYPING_DEBOUNCE_MS = 250L
internal const val APPLY_RATING_FILTERS_DEBOUNCE_MS = 300L
internal const val SORT_DEBOUNCE_MS = 100L
internal const val FILTER_DELAY_MS = 100L
internal const val DELAY_REPORT_DIALOG_MS = 500L

internal enum class PopularRange {
    WEEK,
    MONTH,
    YEAR,
}

@Serializable
internal data class BrowseCache(
    val signature: String,
    val videos: List<VideoSearchResult>,
    val hasMore: Boolean,
    val totalResults: Int,
    val timestampMs: Long,
)

internal sealed interface RandomResult {
    data object Locked : RandomResult
    data object WaitForLoad : RandomResult
    data object AlreadyShuffling : RandomResult
    data object NotEnoughVideos : RandomResult
    data object Shuffled : RandomResult
}

internal data class HomeViewState(
    val sortBy: String? = null,
    val browseVideos: List<VideoSearchResult> = emptyList(),
    val isBrowseOrderManual: Boolean = false,
    val isBrowsing: Boolean = false,
    val browseError: String? = null,
    val searchQuery: String = "",
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isShuffling: Boolean = false,
    val totalResults: Int = 0,
    val favoriteIds: Set<String> = emptySet(),
    val isOnline: Boolean = true,
    val selectedContentLanguages: Set<Strings.Language> = setOf(Strings.Language.CROATIAN),
    val featuredVideos: List<VideoSearchResult> = emptyList(),
    val popularVideos: List<VideoSearchResult> = emptyList(),
    val popularRange: PopularRange = PopularRange.WEEK,
    val popularTimeVideosEnabled: Boolean = true,
    val homeCategories: List<HomeCategory> = emptyList(),
    val selectedCategory: String? = null,
    val uiState: HomeScreenUiState = HomeScreenUiState(),
    val browseRatingFilters: BrowseRatingFilters = BrowseRatingFilters(),
    val searchSuggestions: List<String> = emptyList(),
    val languageCodes: List<String> = listOf("hr", "unknown"),
    val autoShuffle: Boolean = true,
    val pendingCloseVideo: VideoSearchResult? = null,
    val pendingCloseSeconds: Int = 0,
    val showCloseChoiceDialog: Boolean = false,
    val clipToPlay: com.youtube.rating.android.data.models.VideoClip? = null,
    val currentPlaybackSeconds: Float = 0f,
    val saintOfDay: SaintOfDayResponse? = null,
    val showSaintDialog: Boolean = false,
    val showSaintFullDialog: Boolean = false,
    val urlDialogPrefill: String = "",
)
