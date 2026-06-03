@file:Suppress("FunctionName")

package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.youtube.rating.android.localization.Strings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.youtube.rating.android.data.models.VideoClip
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import kotlin.math.roundToInt
import com.youtube.rating.core.designsystem.theme.spacing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues

@Composable
internal fun ClipsTabRow(
    selected: com.youtube.rating.android.ui.models.HomeTab,
    onSelect: (com.youtube.rating.android.ui.models.HomeTab) -> Unit
) {
    val spacing = MaterialTheme.spacing
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        shape = RoundedCornerShape(999.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            ClipTabChip(
                text = "Home",
                selected = selected == com.youtube.rating.android.ui.models.HomeTab.Browse,
                outline = outline,
                onClick = { onSelect(com.youtube.rating.android.ui.models.HomeTab.Browse) }
            )
            ClipTabChip(
                text = "Clips",
                selected = selected == com.youtube.rating.android.ui.models.HomeTab.Clips,
                outline = outline,
                onClick = { onSelect(com.youtube.rating.android.ui.models.HomeTab.Clips) }
            )
        }
    }
}

@Composable
private fun ClipTabChip(
    text: String,
    selected: Boolean,
    outline: Color,
    onClick: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "clipTabContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "clipTabContent"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        border = BorderStroke(1.dp, if (selected) containerColor else outline),
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs)
        )
    }
}

@Composable
internal fun ClipsSection(
    clips: List<VideoClip>,
    onPlay: (VideoClip) -> Unit,
    onDelete: (VideoClip) -> Unit,
    onReorder: (List<VideoClip>) -> Unit,
    onShare: (VideoClip) -> Unit
) {
    val gridState = rememberLazyGridState()
    val localClips = remember { mutableStateListOf<VideoClip>() }
    val haptics = LocalHapticFeedback.current
    val spacing = MaterialTheme.spacing

    LaunchedEffect(clips) {
        localClips.clear()
        localClips.addAll(clips)
    }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragStartPointer by remember { mutableStateOf(Offset.Zero) }
    var currentPointer by remember { mutableStateOf(Offset.Zero) }
    var dragItemStartOffset by remember { mutableStateOf(IntOffset.Zero) }

    if (clips.isEmpty()) {
        EmptyClipsState()
        return
    }

    fun findItemIndexAt(position: Offset): Int? {
        val layoutInfo = gridState.layoutInfo
        layoutInfo.visibleItemsInfo.forEach { info ->
            val rect = Rect(
                left = info.offset.x.toFloat(),
                top = info.offset.y.toFloat(),
                right = info.offset.x + info.size.width.toFloat(),
                bottom = info.offset.y + info.size.height.toFloat()
            )
            if (rect.contains(Offset(position.x, position.y))) {
                return info.index
            }
        }
        return null
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(localClips) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragStartPointer = offset
                        currentPointer = offset
                        draggingIndex = findItemIndexAt(position = offset)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragItemStartOffset = draggingIndex?.let { index ->
                            val info = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                            info?.offset ?: IntOffset.Zero
                        } ?: IntOffset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        currentPointer += dragAmount
                        val fromIndex = draggingIndex
                        if (fromIndex != null) {
                            val overIndex = findItemIndexAt(position = currentPointer)
                            if (overIndex != null && overIndex != fromIndex &&
                                overIndex in localClips.indices && fromIndex in localClips.indices
                            ) {
                                val moved = localClips.removeAt(fromIndex)
                                localClips.add(overIndex, moved)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                draggingIndex = overIndex
                            }
                        }
                    },
                    onDragEnd = {
                        draggingIndex = null
                        onReorder(localClips.toList())
                    },
                    onDragCancel = { draggingIndex = null }
                )
            }
    ) {
        itemsIndexed(localClips, key = { _, item -> item.id }) { index, clip ->
            val isDragging = draggingIndex == index
            val dragScale by animateFloatAsState(
                targetValue = if (isDragging) 1.03f else 1f,
                label = "dragScale"
            )
            val dragElevation by animateDpAsState(
                targetValue = if (isDragging) 10.dp else 2.dp,
                label = "dragElevation"
            )
            val translation = if (isDragging) {
                val dx = currentPointer.x - dragStartPointer.x
                val dy = currentPointer.y - dragStartPointer.y
                IntOffset(dx.roundToInt(), dy.roundToInt()) + dragItemStartOffset
            } else {
                IntOffset.Zero
            }

            ClipCard(
                clip = clip,
                onPlay = { onPlay(clip) },
                onDelete = { onDelete(clip) },
                onShare = { onShare(clip) },
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationX = translation.x.toFloat()
                        translationY = translation.y.toFloat()
                        scaleX = dragScale
                        scaleY = dragScale
                    }
            , elevation = dragElevation)
        }
    }
}

@Composable
private fun ClipCard(
    clip: VideoClip,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp = 2.dp
) {
    val spacing = MaterialTheme.spacing
    val cardShape = MaterialTheme.shapes.medium
    val overlay = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
        startY = 80f
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = clip.thumbnail,
                contentDescription = clip.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(overlay)
            )
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(50))
                    .padding(6.dp)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(spacing.sm)
            ) {
                Text(
                    text = clip.title.ifBlank { "Clip" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val channel = clip.channelName.ifBlank { "Nepoznat kanal" }
                Text(
                    text = channel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            val duration = (clip.endSeconds - clip.startSeconds).coerceAtLeast(0)
            DurationBadge(duration = duration)
            DragHandle()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${clip.startSeconds.formatAsClock()} - ${clip.endSeconds.formatAsClock()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Podijeli isječak")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Obriši isječak")
                }
            }
        }
    }
}

@Composable
internal fun ClipPlayerDialog(
    clip: VideoClip,
    onDismiss: () -> Unit,
    onMiniplayer: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = clip.title.ifBlank { "Clip" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onMiniplayer) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Mini player")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zatvori")
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${clip.startSeconds.formatAsClock()} - ${clip.endSeconds.formatAsClock()} · ${clip.channelName.ifBlank { "Nepoznat kanal" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(Color.Black)
                ) {
                    YouTubePlayerEmbed(
                        videoId = clip.videoId,
                        startSeconds = clip.startSeconds,
                        endSeconds = clip.endSeconds,
                        thumbnail = clip.thumbnail,
                        autoPlay = true
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyClipsState() {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.xl, vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AssistChip(
            onClick = {},
            label = { Text(Strings.noSavedClips) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        Text(
            text = "Spremi trenutke koje voliš i povuci kartice za promjenu redoslijeda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Savjet: duži pritisak pokreće povlačenje.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BoxScope.DurationBadge(duration: Int) {
    val spacing = MaterialTheme.spacing
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(spacing.sm)
            .background(Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = spacing.sm, vertical = spacing.xs)
    ) {
        Text(
            text = duration.formatAsClock(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun BoxScope.DragHandle() {
    val spacing = MaterialTheme.spacing
    Icon(
        imageVector = Icons.Default.DragHandle,
        contentDescription = "Povuci za promjenu redoslijeda",
        tint = Color.White.copy(alpha = 0.8f),
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(spacing.sm)
            .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = spacing.xs, vertical = spacing.xs)
            .size(18.dp)
    )
}

private fun Int.formatAsClock(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%d:%02d".format(minutes, seconds)
}
