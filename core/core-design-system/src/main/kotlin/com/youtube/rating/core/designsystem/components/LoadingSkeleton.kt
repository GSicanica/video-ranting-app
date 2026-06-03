package com.youtube.rating.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.youtube.rating.core.designsystem.theme.spacing

@Composable
fun VideoCardSkeleton(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
) {
    // Backward compatibility: use grid skeleton styling
    VideoGridCardSkeleton(modifier = modifier)
}

@Composable
fun VideoGridCardSkeleton(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(190.dp)
) {
    val shimmerBrush = rememberShimmerBrush()
    val cardShape = MaterialTheme.shapes.medium
    val spacing = MaterialTheme.spacing

    Card(
        modifier = modifier,
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(shimmerBrush)
            )

            // Text & badges placeholders
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.sm, vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                skeletonLine(widthFraction = 0.92f, height = 12.dp, brush = shimmerBrush)
                skeletonLine(widthFraction = 0.7f, height = 10.dp, brush = shimmerBrush)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .height(14.dp)
                                .width(44.dp)
                                .background(shimmerBrush, RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoListCardSkeleton(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(132.dp)
) {
    val shimmerBrush = rememberShimmerBrush()
    val cardShape = MaterialTheme.shapes.medium
    val spacing = MaterialTheme.spacing

    Card(
        modifier = modifier,
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .background(shimmerBrush, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = spacing.sm, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                skeletonLine(widthFraction = 0.95f, height = 13.dp, brush = shimmerBrush)
                skeletonLine(widthFraction = 0.7f, height = 11.dp, brush = shimmerBrush)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .height(14.dp)
                                .width(42.dp)
                                .background(shimmerBrush, RoundedCornerShape(10.dp))
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(width = 26.dp, height = 12.dp)
                            .background(shimmerBrush, RoundedCornerShape(6.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun skeletonLine(
    widthFraction: Float,
    height: Dp = 12.dp,
    brush: Brush,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .background(brush, shape)
    )
}

@Composable
private fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    // Animated shimmer effect
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1200,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    return remember(base, highlight, shimmerProgress) {
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(-300f + shimmerProgress * 1100f, 0f),
            end = Offset(500f + shimmerProgress * 1100f, 0f)
        )
    }
}

@Composable
fun LoadingScreen(count: Int = 3) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(count) {
            VideoGridCardSkeleton()
        }
    }
}
