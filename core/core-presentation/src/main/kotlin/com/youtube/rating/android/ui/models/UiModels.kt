package com.youtube.rating.android.ui.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import com.youtube.rating.android.util.VideoSource
import com.youtube.rating.shared.models.VideoSearchResult
import kotlinx.serialization.Serializable

@Serializable
enum class HomeTab {
    Browse,
    ForYou,
    Clips
}

/**
 * UI Models for Android app - shared data classes for composables
 */

@Stable
data class RatingDraft(
    val url: String = "",
    val source: VideoSource = VideoSource.Unknown(),
    val love: Int = 0,
    val faith: Int = 0,
    val hope: Int = 0,
    val category: String? = null,
    val manualLanguage: String = "",
    val facebookUrl: String? = null // Store Facebook URL for video player
)

val RatingDraftStateSaver: Saver<MutableState<RatingDraft>, List<Any?>> = Saver(
    save = { s ->
        listOf(
            s.value.url,
            s.value.love,
            s.value.faith,
            s.value.hope,
            s.value.category,
            s.value.manualLanguage,
            s.value.facebookUrl
        )
    },
    restore = { l ->
        mutableStateOf(
            RatingDraft(
                url = l.getOrNull(0) as? String ?: "",
                source = VideoSource.Unknown(),
                love = l.getOrNull(1) as? Int ?: 0,
                faith = l.getOrNull(2) as? Int ?: 0,
                hope = l.getOrNull(3) as? Int ?: 0,
                category = l.getOrNull(4) as? String,
                manualLanguage = l.getOrNull(5) as? String ?: "",
                facebookUrl = l.getOrNull(6) as? String
            )
        )
    }
)

@Stable
@Serializable
data class AsyncDialogState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: String? = null
) {
    fun reset() = copy(loading = false, error = null, success = null)
    fun start() = copy(loading = true, error = null, success = null)
    fun fail(msg: String) = copy(loading = false, error = msg, success = null)
    fun ok(msg: String) = copy(loading = false, error = null, success = msg)
}

@Stable
@Serializable
data class QuickRateDraft(
    val love: Int = 0,
    val faith: Int = 0,
    val hope: Int = 0
)

@Stable
@Serializable
data class BrowseRatingFilters(
    val loveMin: Int = 0,
    val faithMin: Int = 0,
    val hopeMin: Int = 0
)

@Stable
@Serializable
data class HomeScreenUiState(
    val showUrlInputDialog: Boolean = false,
    val showVideoDetailsDialog: VideoSearchResult? = null,
    val showReportDialog: VideoSearchResult? = null,
    val showDeleteDialog: VideoSearchResult? = null,
    val quickRateVideo: VideoSearchResult? = null,
    val quickRateDraft: QuickRateDraft = QuickRateDraft(),
    val quickActionsVideo: VideoSearchResult? = null,
    val reportState: AsyncDialogState = AsyncDialogState(),
    val deleteState: AsyncDialogState = AsyncDialogState(),
    val showBrowseControlsSheet: Boolean = false,
    val showHomeControlsSheet: Boolean = false,
    val showBrowse: Boolean = false,
    val selectedTab: HomeTab = HomeTab.Browse,
    val isGridView: Boolean = true,
    val isFullScreenVideoOpen: Boolean = false,
    val showSearchField: Boolean = false,
    val gridFirstVisibleItemIndex: Int = 0,
    val gridFirstVisibleItemScrollOffset: Int = 0,
    val listFirstVisibleItemIndex: Int = 0,
    val listFirstVisibleItemScrollOffset: Int = 0
)
