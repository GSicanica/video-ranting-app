package com.youtube.rating.android.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.designsystem.theme.spacing
import com.youtube.rating.android.util.formatRelativeTime
import com.youtube.rating.android.utils.ThumbnailHelper
import com.youtube.rating.shared.models.VideoSearchResult
import kotlinx.coroutines.delay
import java.util.Locale

private val CardRadius = 16.dp
private val CardElevation = 2.dp

@Composable
fun VideoGridCard(
    video: VideoSearchResult,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onReport: () -> Unit = {},
    videoIsDisabled: Boolean = false,
    isFavorite: Boolean = false,
    isScrolling: Boolean = false,
    allowOverlays: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val spacing = MaterialTheme.spacing

    // Avoid stale lambdas captured by recompositions
    val onClickState by rememberUpdatedState(onClick)
    val onLongPressState by rememberUpdatedState(onLongPress)
    val onFavoriteClickState by rememberUpdatedState(onFavoriteClick)
    val onReportState by rememberUpdatedState(onReport)

    val cardShape = remember { RoundedCornerShape(CardRadius) }
    val imageShape = remember { RoundedCornerShape(topStart = CardRadius, topEnd = CardRadius) }

    val overlaySurfaceColor = remember { Color.Black.copy(alpha = 0.55f) }
    val overlayContentColor = remember { Color.White }

    val loveColor = MaterialTheme.colorScheme.tertiary
    val faithColor = MaterialTheme.colorScheme.primary
    val hopeColor = MaterialTheme.colorScheme.secondary

    val thumbCandidates = remember(video.videoId, video.thumbnail) {
        ThumbnailHelper
            .buildThumbnailCandidates(video.videoId, video.thumbnail)
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf(video.thumbnail).filter { it.isNotBlank() } }
    }
    var thumbIndex by remember(video.videoId, video.thumbnail) { mutableIntStateOf(0) }

    LaunchedEffect(thumbCandidates) { thumbIndex = 0 }

    val currentThumbUrl by remember(thumbCandidates, thumbIndex) {
        derivedStateOf { thumbCandidates.getOrElse(thumbIndex) { thumbCandidates.lastOrNull().orEmpty() } }
    }

    val imageRequest = rememberThumbnailRequest(context = context, url = currentThumbUrl)

    val titleText by remember(video.title) {
        derivedStateOf { video.title.ifBlank { "Bez naslova" } }
    }
    val channelText by remember(video.channelName) {
        derivedStateOf { video.channelName.ifBlank { "Nepoznat kanal" } }
    }
    val metaText by remember(video.category, video.createdAt) {
        derivedStateOf {
            buildList {
                val cat = video.category?.trim().orEmpty()
                if (cat.isNotEmpty()) add(cat)
                if (video.createdAt > 0L) add(video.createdAt.formatRelativeTime())
            }.joinToString(" • ")
        }
    }

    val canInteract = !isScrolling && !videoIsDisabled

    val overlayAlpha by animateFloatAsState(
        targetValue = if (allowOverlays && !isScrolling) 1f else 0f,
        animationSpec = tween(180),
        label = "overlayAlpha"
    )

    // Heart burst trigger
    var burstTick by remember { mutableIntStateOf(0) }

    fun likeAction() {
        if (!canInteract) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        burstTick++
        onFavoriteClickState()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (!canInteract) disabled() }
            .combinedClickable(
                enabled = canInteract,
                onClick = { onClickState() },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPressState()
                },
                onDoubleClick = { likeAction() }
            ),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // -------------------- IMAGE --------------------
            Box(modifier = Modifier.fillMaxWidth()) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = titleText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(imageShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    onError = {
                        if (thumbIndex < thumbCandidates.lastIndex) thumbIndex += 1
                    },
                    loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                    success = { SubcomposeAsyncImageContent() }
                )

                HeartBurstOverlay(
                    visibleKey = burstTick,
                    modifier = Modifier.fillMaxSize()
                )

                if (allowOverlays) {
                    OverlayIconButton(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .alpha(overlayAlpha),
                        enabled = canInteract,
                        backgroundColor = overlaySurfaceColor,
                        contentColor = overlayContentColor,
                        onClick = { onReportState() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Prijavi video",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OverlayIconButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .alpha(overlayAlpha),
                        enabled = canInteract,
                        backgroundColor = overlaySurfaceColor,
                        contentColor = overlayContentColor,
                        onClick = { likeAction() }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) Strings.removeFromFavorites else Strings.addToFavorites,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (videoIsDisabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Onemogućen",
                            color = Color.White,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // -------------------- TEXT --------------------
            AnimatedVisibility(
                visible = !isScrolling,
                enter = androidx.compose.animation.fadeIn(tween(140)) +
                        androidx.compose.animation.slideInVertically(tween(240)) { it / 2 },
                exit = androidx.compose.animation.fadeOut(tween(90)) +
                        androidx.compose.animation.slideOutVertically(tween(200)) { it / 2 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.sm, vertical = spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = channelText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = video.totalRatings.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (metaText.isNotBlank()) {
                        Text(
                            text = metaText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val whyTagText = video.whyTag?.trim().orEmpty()
                    if (whyTagText.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = whyTagText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs)
                            )
                        }
                    }

                    // ✅ FIX: badges uvijek u 1 redu + jednaka širina + bez prelamanja
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RatingPill(
                            emoji = "❤️",
                            value = video.avgLove,
                            color = loveColor,
                            modifier = Modifier.weight(1f)
                        )
                        RatingPill(
                            emoji = "✝️",
                            value = video.avgFaith,
                            color = faithColor,
                            modifier = Modifier.weight(1f)
                        )
                        RatingPill(
                            emoji = "⭐",
                            value = video.avgHope,
                            color = hopeColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayIconButton(
    modifier: Modifier,
    enabled: Boolean,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = backgroundColor
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(36.dp)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalContentColor provides contentColor,
                content = content
            )
        }
    }
}

@Composable
private fun RatingPill(
    emoji: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val container = color.copy(alpha = 0.14f)
    val border = color.copy(alpha = 0.28f)
    val valueText = remember(value) { value.toOneDecimalComma() }

    Surface(
        modifier = modifier.heightIn(min = 34.dp),
        shape = RoundedCornerShape(10.dp),
        color = container,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.22f)
            ) {
                Box(
                    modifier = Modifier.size(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

private fun Double.toOneDecimalComma(): String {
    // 3.0 -> "3,0"
    val s = String.format(Locale.US, "%.1f", this)
    return s.replace('.', ',')
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    val brush = remember(x, base, highlight) {
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = androidx.compose.ui.geometry.Offset(x - 1000f, 0f),
            end = androidx.compose.ui.geometry.Offset(x, 0f)
        )
    }

    Box(modifier = modifier.background(brush))
}

@Composable
private fun HeartBurstOverlay(
    visibleKey: Int,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(visibleKey) {
        if (visibleKey == 0) return@LaunchedEffect
        visible = true
        scale.snapTo(0.6f)
        alpha.snapTo(0f)

        alpha.animateTo(1f, tween(120))
        scale.animateTo(1.1f, tween(140))
        delay(120)
        alpha.animateTo(0f, tween(220))
        scale.animateTo(1.35f, tween(240))
        visible = false
    }

    AnimatedVisibility(visible = visible, modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier
                    .size(54.dp)
                    .alpha(alpha.value)
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            )
        }
    }
}

@Composable
private fun rememberThumbnailRequest(
    context: Context,
    url: String
): ImageRequest {
    return remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(width = 320, height = 180)
            .scale(Scale.FILL)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .error(android.R.drawable.ic_menu_gallery)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .build()
    }
}
