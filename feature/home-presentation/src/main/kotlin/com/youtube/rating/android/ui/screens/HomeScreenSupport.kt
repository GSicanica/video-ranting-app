package com.youtube.rating.android.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.designsystem.components.VideoCardSkeleton
import com.youtube.rating.android.ui.models.BrowseRatingFilters
import com.youtube.rating.android.ui.models.RatingDraft
import com.youtube.rating.android.ui.models.RatingDraftStateSaver

@Stable
internal fun Context.findActivityOrNull(): ComponentActivity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    com.youtube.rating.android.sentry.SentryLogger.captureMessage(
        "HomeScreen: Context is not a ComponentActivity",
        tags = mapOf("context_class" to this::class.java.name)
    )
    return null
}

internal fun Context.safeStartActivity(intent: Intent) {
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

internal fun Context.safeStartChooser(sendIntent: Intent, chooserTitle: String) {
    val chooser = Intent.createChooser(sendIntent, chooserTitle)
    if (this !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(chooser)
}

@Composable
internal fun rememberRatingDraft(): MutableState<RatingDraft> =
    rememberSaveable(saver = RatingDraftStateSaver) { mutableStateOf(RatingDraft()) }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GridSkeleton(count: Int = 4) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(
            count = count,
            contentType = { "video_skeleton" },
            key = { it }
        ) {
            VideoCardSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.0f)
            )
        }
    }
}

@Composable
internal fun ListSkeleton(
    count: Int = 4,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 80.dp,
) {
    val listState = rememberLazyListState()

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 250),
        label = "skeletonAlpha"
    )

    val heights = remember(count) {
        List(count) { index ->
            when (index % 3) {
                0 -> 132.dp
                1 -> 140.dp
                else -> 156.dp
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 4.dp,
            bottom = bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = count,
            key = { index -> "video_skeleton_$index" },
            contentType = { "video_skeleton" }
        ) { index ->
            VideoCardSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heights[index])
            )
        }
    }
}

@Composable
internal fun CompactRatingFiltersRow(
    ratingFilters: BrowseRatingFilters,
    onRatingFiltersChange: (BrowseRatingFilters) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            Strings.minRatings,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                @Composable
                fun RatingRow(
                    emoji: String,
                    label: String,
                    current: Int,
                    onChange: (Int) -> Unit
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(0, 1, 2, 3).forEach { value ->
                                val selected = value == current
                                FilterChip(
                                    selected = selected,
                                    onClick = { onChange(value) },
                                    label = {
                                        Text(
                                            text = if (value == 0) Strings.allText else value.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }
                }

                RatingRow(
                    emoji = "❤️",
                    label = Strings.ratingTypeLove.substring(3),
                    current = ratingFilters.loveMin,
                    onChange = { onRatingFiltersChange(ratingFilters.copy(loveMin = it)) }
                )

                RatingRow(
                    emoji = "✝️",
                    label = Strings.ratingTypeFaith.substring(3),
                    current = ratingFilters.faithMin,
                    onChange = { onRatingFiltersChange(ratingFilters.copy(faithMin = it)) }
                )

                RatingRow(
                    emoji = "⭐",
                    label = Strings.ratingTypeHope,
                    current = ratingFilters.hopeMin,
                    onChange = { onRatingFiltersChange(ratingFilters.copy(hopeMin = it)) }
                )
            }
        }
    }
}
