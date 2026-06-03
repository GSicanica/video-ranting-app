package com.youtube.rating.android.widget

import com.youtube.rating.core.coroutines.ioDispatcher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.RemoteViews
import coil.imageLoader
import coil.request.ImageRequest
import com.youtube.rating.core.data.R
import com.youtube.rating.shared.data.FavoriteItemTypeModels
import com.youtube.rating.shared.data.FavoritesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named

class FavoriteVideoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(ioDispatcher).launch {
            try {
                val koin = GlobalContext.getOrNull() ?: return@launch
                val favoritesRepo: FavoritesRepository = koin.get(qualifier = named("sharedFavoritesRepo"))
                val favorites = favoritesRepo.getAllFavorites().first()
                val favorite = favorites
                    .filter { it.type == FavoriteItemTypeModels.VIDEO }
                    .sortedByDescending { it.timestamp }
                    .firstOrNull()
                val thumbnail = favorite?.thumbnail?.takeIf { it.isNotBlank() }
                val bitmap = if (thumbnail != null) loadThumbnail(context = context, url = thumbnail) else null

                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.favorite_video_widget)
                    if (favorite == null) {
                        views.setTextViewText(
                            R.id.widget_title,
                            context.getString(R.string.widget_no_favorites)
                        )
                        views.setTextViewText(
                            R.id.widget_channel,
                            context.getString(R.string.widget_add_favorite_hint)
                        )
                        views.setImageViewResource(R.id.widget_thumbnail, R.mipmap.ic_launcher)
                    } else {
                        views.setTextViewText(R.id.widget_title, favorite.title)
                        views.setTextViewText(R.id.widget_channel, favorite.channelName)
                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.widget_thumbnail, bitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_thumbnail, R.mipmap.ic_launcher)
                        }
                    }

                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("youtuberating://rate")
                    ).apply {
                        setPackage(context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.favorite_video_widget)
                    views.setTextViewText(
                        R.id.widget_title,
                        context.getString(R.string.widget_no_favorites)
                    )
                    views.setTextViewText(
                        R.id.widget_channel,
                        context.getString(R.string.widget_add_favorite_hint)
                    )
                    views.setImageViewResource(R.id.widget_thumbnail, R.mipmap.ic_launcher)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, FavoriteVideoWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                val intent = Intent(context, FavoriteVideoWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }

        private suspend fun loadThumbnail(context: Context, url: String): Bitmap? {
            // Use the app-wide Coil ImageLoader (respects ImageLoaderFactory + interceptors).
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(480, 270)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            return (result.drawable as? BitmapDrawable)?.bitmap
        }
    }
}
