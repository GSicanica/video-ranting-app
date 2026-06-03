package com.youtube.rating.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.youtube.rating.shared.models.VideoSearchResult
import kotlin.math.roundToInt

@Composable
fun FloatingVideoPlayer(
    video: VideoSearchResult,
    startSeconds: Int,
    modifier: Modifier = Modifier,
    onExpand: (Int) -> Unit,
    onClose: () -> Unit
) {
    val cardWidth = 280.dp
    val cardHeight = cardWidth * 9 / 16
    val margin = 12.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxX = with(density) { (constraints.maxWidth.toFloat() - cardWidth.toPx() - margin.toPx()) }
        val maxY = with(density) { (constraints.maxHeight.toFloat() - cardHeight.toPx() - margin.toPx()) }
        val clampXMax = maxX.coerceAtLeast(0f)
        val clampYMax = maxY.coerceAtLeast(0f)

        var offset by remember { mutableStateOf(Offset(clampXMax, clampYMax)) }
        var initialized by remember { mutableStateOf(false) }
        var currentSecond by remember { mutableStateOf(startSeconds.coerceAtLeast(0)) }

        LaunchedEffect(constraints.maxWidth, constraints.maxHeight) {
            if (!initialized) {
                offset = Offset(clampXMax, clampYMax)
                initialized = true
            } else {
                offset = Offset(
                    offset.x.coerceIn(0f, clampXMax),
                    offset.y.coerceIn(0f, clampYMax)
                )
            }
        }

        val clamped = Offset(
            offset.x.coerceIn(0f, clampXMax),
            offset.y.coerceIn(0f, clampYMax)
        )

        Surface(
            modifier = Modifier
                .offset { IntOffset(clamped.x.roundToInt(), clamped.y.roundToInt()) }
                .size(cardWidth, cardHeight)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offset = Offset(offset.x + dragAmount.x, offset.y + dragAmount.y)
                    }
                },
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                YouTubePlayerEmbed(
                    videoId = video.videoId,
                    startSeconds = startSeconds,
                    autoPlay = true,
                    onCurrentSecond = { second ->
                        currentSecond = second.toInt().coerceAtLeast(0)
                    }
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    IconButton(onClick = { onExpand(currentSecond) }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Otvori player",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Zatvori",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
