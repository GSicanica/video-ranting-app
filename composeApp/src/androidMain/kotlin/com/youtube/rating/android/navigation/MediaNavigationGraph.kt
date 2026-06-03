package com.youtube.rating.android.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.youtube.rating.android.ui.screens.GalleryScreen
import com.youtube.rating.android.ui.screens.OfflineVideosScreen
import com.youtube.rating.android.ui.screens.RatedVideosScreen
import com.youtube.rating.android.ui.screens.WatchHistoryScreen
import com.youtube.rating.android.storage.OfflineVideo

fun NavGraphBuilder.mediaNavigationGraph(
    navCoordinator: NavigationCoordinator,
    onPlayVideo: (OfflineVideo) -> Unit = {}
) {
    // Rated Videos Screen
    composable(Screen.RatedVideos.route) {
        RatedVideosScreen()
    }

    // Watch History Screen
    composable(Screen.WatchHistory.route) {
        WatchHistoryScreen(
            onVideoClick = { videoId ->
                navCoordinator.navigateToRateVideo(videoId)
            }
        )
    }

    // Offline Videos Screen
    composable(Screen.OfflineVideos.route) {
        OfflineVideosScreen(
            onPlayVideo = onPlayVideo,
            onOpenGallery = { navCoordinator.navigateToGallery() }
        )
    }

    // Gallery Screen
    composable(Screen.Gallery.route) {
        GalleryScreen()
    }

}
