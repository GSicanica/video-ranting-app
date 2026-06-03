package com.youtube.rating.android.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Stable
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.ui.models.QuickRateDraft
import com.youtube.rating.android.utils.HapticFeedback
import com.youtube.rating.android.viewmodel.HomeViewModel
import com.youtube.rating.shared.models.VideoSearchResult

/**
 * Callback factories for HomeScreen
 * Groups related callbacks to reduce parameter passing and improve organization
 */
@Stable
class HomeScreenCallbacks(
    private val context: Context,
    private val homeViewModel: HomeViewModel,
    private val haptic: HapticFeedback,
    private val favoriteSet: Set<String>,
    private val refreshBrowse: (resetPage: Boolean) -> Unit
) {

    /**
     * Handle video card click - opens video details dialog
     */
    val onVideoClick: (VideoSearchResult) -> Unit = { video ->
        homeViewModel.updateUiState { it.copy(showVideoDetailsDialog = video) }
    }

    /**
     * Handle report icon click - opens report dialog
     */
    val onReportClick: (VideoSearchResult) -> Unit = { video ->
        homeViewModel.updateUiState { it.copy(showReportDialog = video) }
    }

    /**
     * Handle quick rate button click - opens quick rate sheet
     */
    val onQuickRateClick: (VideoSearchResult) -> Unit = { video ->
        homeViewModel.updateUiState {
            it.copy(
                quickRateDraft = QuickRateDraft(),
                quickRateVideo = video
            )
        }
    }

    /**
     * Handle long press on video card - opens quick actions sheet
     */
    val onLongPressVideo: (VideoSearchResult) -> Unit = { video ->
        if (BuildConfig.DEBUG) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("video_id", video.videoId)
            clipboard?.setPrimaryClip(clip)
           // Toast.makeText(context, "Kopiran ID: ${video.videoId}", Toast.LENGTH_SHORT).show()
        }
        homeViewModel.updateUiState { it.copy(quickActionsVideo = video) }
    }

    /**
     * Toggle favorite status for a video
     * Provides haptic feedback on success
     */
    val toggleFavorite: (VideoSearchResult) -> Unit = { video ->
        if (video.videoId in favoriteSet) {
            homeViewModel.removeFromFavorites(video.videoId) { success ->
                if (success) haptic.lightTap()
            }
        } else {
            homeViewModel.addToFavorites(video) { success ->
                if (success) haptic.lightTap()
            }
        }
    }

    /**
     * Reset all filters and refresh browse
     * Provides haptic feedback
     */
    val onResetFilters: () -> Unit = {
        haptic.click()
        homeViewModel.resetBrowseRatingFilters()
        homeViewModel.resetFilters()
        refreshBrowse(true)
    }

    /**
     * Retry loading browse videos
     */
    val onRetry: () -> Unit = {
        refreshBrowse(true)
    }
}
