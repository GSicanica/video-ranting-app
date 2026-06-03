package com.youtube.rating.android.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.youtube.rating.android.core.AppKeys
import com.youtube.rating.android.utils.DeepLinkUtil

class NotificationRouterImpl : NotificationRouter {
    private fun createOpenScreenIntent(
        context: Context,
        destination: NotificationDestination
    ): Intent {
        val key = when (destination) {
            NotificationDestination.GOSPEL_DAY -> AppKeys.OpenScreens.GOSPEL_DAY
            NotificationDestination.TRAINING -> AppKeys.OpenScreens.TRAINING
        }
        val uri = Uri.Builder()
            .scheme("youtuberating")
            .authority("rate")
            .appendQueryParameter(AppKeys.IntentExtras.OPEN_SCREEN, key)
            .build()

        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    override fun createContentPendingIntent(
        context: Context,
        destination: NotificationDestination,
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

    override fun createOpenVideoPendingIntent(
        context: Context,
        videoId: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinkUtil.getWatchUrl(videoId))).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun resolveDestination(intent: Intent?): NotificationDestination? {
        val key = intent?.getStringExtra(AppKeys.IntentExtras.OPEN_SCREEN)
            ?: intent?.data?.getQueryParameter(AppKeys.IntentExtras.OPEN_SCREEN)
        return when (key) {
            AppKeys.OpenScreens.GOSPEL_DAY -> NotificationDestination.GOSPEL_DAY
            AppKeys.OpenScreens.TRAINING -> NotificationDestination.TRAINING
            else -> null
        }
    }
}
