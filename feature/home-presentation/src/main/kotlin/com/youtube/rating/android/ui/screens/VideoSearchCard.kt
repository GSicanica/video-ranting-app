package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.youtube.rating.android.utils.CompactRatingBadge
import com.youtube.rating.android.utils.ThumbnailHelper
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.designsystem.theme.spacing
import com.youtube.rating.android.util.formatRelativeTime
import com.youtube.rating.shared.models.VideoSearchResult

private val CardRadius = 16.dp

@Composable
fun VideoSearchCard(
    video: VideoSearchResult,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onQuickRateClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    isFavorite: Boolean = false,
    onDelete: () -> Unit = {},
    onReport: () -> Unit = {},
    isAdminMode: Boolean = false,
    isScrolling: Boolean = false
) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val cardShape = RoundedCornerShape(CardRadius)
    val imageShape = RoundedCornerShape(topStart = CardRadius, bottomStart = CardRadius)
    val thumbCandidates = remember(video.videoId, video.thumbnail) {
        ThumbnailHelper.buildThumbnailCandidates(video.videoId, video.thumbnail)
    }
    var thumbIndex by remember(video.videoId, video.thumbnail) { mutableIntStateOf(0) }
    val thumbnailUrl = thumbCandidates.getOrElse(thumbIndex) { thumbCandidates.lastOrNull().orEmpty() }
    val imageRequest = remember(context, thumbnailUrl) {
        val targetWidth = 320
        val targetHeight = 180
        ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .size(targetWidth, targetHeight)
            .scale(coil.size.Scale.FIT)  // Better quality at smaller size
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .error(android.R.drawable.ic_menu_gallery)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .fallback(android.R.drawable.ic_menu_gallery)
            .build()
    }

    val titleText = remember(video.title) { video.title.ifBlank { "Bez naslova" } }
    val channelText = remember(video.channelName) { video.channelName.ifBlank { "Nepoznat kanal" } }
    val metaText = remember(video.language, video.category, video.createdAt) {
        buildList {
            val lang = video.language.trim()
            if (lang.isNotEmpty() && lang.lowercase() != "unknown") add(lang.uppercase())
            val cat = video.category?.trim().orEmpty()
            if (cat.isNotEmpty()) add(cat)
            if (video.createdAt > 0L) add(video.createdAt.formatRelativeTime())
        }.joinToString(" • ")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress, onDoubleClick = onFavoriteClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left: Thumbnail + report icon (top-left)
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = titleText,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(imageShape),
                    contentScale = ContentScale.Crop,
                    onError = {
                        if (thumbIndex < thumbCandidates.lastIndex) {
                            thumbIndex += 1
                        }
                    }
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(spacing.xs),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
                ) {
                    IconButton(
                        onClick = onReport,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Prijavi video",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (!isScrolling && metaText.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(spacing.xs),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = metaText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs)
                        )
                    }
                }
            }

            // Right: Info (stable layout even while scrolling)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = spacing.sm, vertical = spacing.sm),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    if (!isScrolling) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = channelText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp)
                        ) {}
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp)
                        ) {}
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactRatingBadge(
                        emoji = "\u2764\uFE0F",
                        value = video.avgLove,
                        color = Color(0xFFE57373)
                    )
                    CompactRatingBadge(
                        emoji = "\u271D\uFE0F",
                        value = video.avgFaith,
                        color = Color(0xFF4DD0E1)
                    )
                    CompactRatingBadge(
                        emoji = "\u2B50",
                        value = video.avgHope,
                        color = Color(0xFFFFD54F)
                    )
                    if (!isScrolling) {
                        val ratingLabel = if (video.totalRatings == 1) {
                            "1 ocjena"
                        } else {
                            "${video.totalRatings} ocjena"
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = ratingLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        IconButton(
                            onClick = onQuickRateClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Brza ocjena",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        IconButton(
                            onClick = onFavoriteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) Strings.removeFromFavorites else Strings.addToFavorites,
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (isAdminMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Obrisi (admin)",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
