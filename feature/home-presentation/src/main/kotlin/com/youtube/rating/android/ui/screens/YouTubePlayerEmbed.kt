package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.graphics.Rect
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@JvmOverloads
@Composable
fun YouTubePlayerEmbed(
    videoId: String,
    startSeconds: Int = 0,
    endSeconds: Int? = null,
    thumbnail: String? = null,
    autoPlay: Boolean = true,
    onViewRectChanged: (Rect?) -> Unit = {},
    onPlayerReady: (YouTubePlayer) -> Unit = {},
    onCurrentSecond: (Float) -> Unit = {}
) {
    var isLoaded by remember { mutableStateOf(false) }
    var stoppedAtEnd by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentVideoId by rememberUpdatedState(videoId)
    val currentStart by rememberUpdatedState(startSeconds.coerceAtLeast(0))
    val safeEnd = rememberUpdatedState(
        endSeconds?.takeIf { it > currentStart } ?: (currentStart + 1)
    )
    var youTubePlayerView by remember { mutableStateOf<YouTubePlayerView?>(null) }
    var youTubePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }
    var lastLoaded by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var lastRect by remember { mutableStateOf<Rect?>(null) }
    val currentOnPlayerReady by rememberUpdatedState(onPlayerReady)
    val currentOnCurrentSecond by rememberUpdatedState(onCurrentSecond)
    val currentOnViewRectChanged by rememberUpdatedState(onViewRectChanged)

    LaunchedEffect(videoId, startSeconds) {
        isLoaded = false
        stoppedAtEnd = false
        lastLoaded = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)
                    enableAutomaticInitialization = false

                    val options = IFramePlayerOptions.Builder(ctx)
                        // Force controls ON to stay compliant with YouTube UI/branding expectations.
                        .controls(1)
                        .rel(0)
                        .ivLoadPolicy(3)
                        .ccLoadPolicy(0)
                        .build()

                    initialize(object : AbstractYouTubePlayerListener() {
                        override fun onReady(player: YouTubePlayer) {
                            youTubePlayer = player
                            isLoaded = true
                            stoppedAtEnd = false
                            lastLoaded = currentVideoId to currentStart
                            currentOnPlayerReady(player)
                            if (autoPlay) {
                                player.loadVideo(currentVideoId, currentStart.toFloat())
                            } else {
                                player.cueVideo(currentVideoId, currentStart.toFloat())
                            }
                        }

                        override fun onCurrentSecond(player: YouTubePlayer, second: Float) {
                            currentOnCurrentSecond(second)
                            val end = safeEnd.value
                            if (endSeconds != null && !stoppedAtEnd && second >= end) {
                                stoppedAtEnd = true
                                player.pause()
                            }
                        }
                    }, options)

                    youTubePlayerView = this
                }
            },
            update = {
                if (it.width > 0 && it.height > 0) {
                    val loc = IntArray(2)
                    it.getLocationInWindow(loc)
                    val rect = Rect(
                        loc[0],
                        loc[1],
                        loc[0] + it.width,
                        loc[1] + it.height
                    )
                    if (rect != lastRect) {
                        lastRect = rect
                        currentOnViewRectChanged(rect)
                    }
                } else if (lastRect != null) {
                    lastRect = null
                    currentOnViewRectChanged(null)
                }
                val player = youTubePlayer
                val target = currentVideoId to currentStart
                if (player != null && lastLoaded != target) {
                    stoppedAtEnd = false
                    lastLoaded = target
                    if (autoPlay) {
                        player.loadVideo(currentVideoId, currentStart.toFloat())
                    } else {
                        player.cueVideo(currentVideoId, currentStart.toFloat())
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isLoaded && thumbnail != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (!isLoaded) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            youTubePlayerView?.let { view ->
                lifecycleOwner.lifecycle.removeObserver(view)
                view.release()
            }
            youTubePlayer = null
            youTubePlayerView = null
            currentOnViewRectChanged(null)
        }
    }
}
