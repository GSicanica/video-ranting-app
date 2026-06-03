package com.youtube.rating.android.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.youtube.rating.android.core.AppKeys
import com.youtube.rating.android.utils.DeepLinkUtil

object NotificationNavigator {
    enum class Destination(
        val key: String,
        val route: String
    ) {
        GOSPEL_DAY(
            key = AppKeys.OpenScreens.GOSPEL_DAY,
            route = "reader/gospel"
        ),
        TRAINING(
            key = AppKeys.OpenScreens.TRAINING,
            route = "training"
        ),
        HABIT_TRACKER(
            key = AppKeys.OpenScreens.HABIT_TRACKER,
            route = "habit_tracker/monthly"
        );

        companion object {
            fun fromKey(key: String?): Destination? = entries.firstOrNull { it.key == key }
        }
    }

    fun createOpenScreenIntent(
        context: Context,
        destination: Destination
    ): Intent {
        val uri = Uri.Builder()
            .scheme("youtuberating")
            .authority("rate")
            .appendQueryParameter(AppKeys.IntentExtras.OPEN_SCREEN, destination.key)
            .build()

        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    fun createContentPendingIntent(
        context: Context,
        destination: Destination,
        requestCode: Int
    ): PendingIntent {
        val intent = createOpenScreenIntent(context = context, destination = destination)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createOpenVideoIntent(
        context: Context,
        videoId: String
    ): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinkUtil.getWatchUrl(videoId))).apply {
        // Route into this app (we also have YouTube URL intent-filters; without package this can be handled elsewhere).
        setPackage(context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    fun createOpenVideoPendingIntent(
        context: Context,
        videoId: String,
        requestCode: Int
    ): PendingIntent {
        val intent = createOpenVideoIntent(context = context, videoId = videoId)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun resolveDestination(intent: Intent?): Destination? {
        val key = intent?.getStringExtra(AppKeys.IntentExtras.OPEN_SCREEN)
            ?: intent?.data?.getQueryParameter(AppKeys.IntentExtras.OPEN_SCREEN)
        return Destination.fromKey(key)
    }
}
