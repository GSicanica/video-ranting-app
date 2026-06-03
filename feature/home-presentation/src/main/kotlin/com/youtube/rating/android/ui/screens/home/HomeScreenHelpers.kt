package com.youtube.rating.android.ui.screens.home

import android.content.Context
import android.content.Intent
import com.youtube.rating.android.data.models.VideoClip
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.ui.screens.safeStartChooser
import com.youtube.rating.android.utils.DeepLinkUtil

/**
 * Helper functions for HomeScreen
 * Pure functions and utilities extracted from the main HomeScreen composable
 */
object HomeScreenHelpers {
    fun shareVideo(context: Context, videoId: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, DeepLinkUtil.getAppShareUrl(videoId))
        }
        context.safeStartChooser(shareIntent, Strings.share)
    }

    fun shareClip(context: Context, clip: VideoClip) {
        val start = clip.startSeconds.coerceAtLeast(0)
        val url = DeepLinkUtil.getAppShareUrl(videoId = clip.videoId, startSeconds = start)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.safeStartChooser(shareIntent, Strings.share)
    }
}
