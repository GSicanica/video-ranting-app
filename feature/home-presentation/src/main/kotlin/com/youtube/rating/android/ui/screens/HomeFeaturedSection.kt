package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.youtube.rating.android.home.PopularRange
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.utils.ThumbnailHelper
import com.youtube.rating.shared.models.VideoSearchResult

// -----------------------------------------------------------------------------
// Featured section – slider with large cards
// -----------------------------------------------------------------------------
@Composable
internal fun FeaturedVideosSection(
    videos: List<VideoSearchResult>,
    popularRange: PopularRange,
    onPopularRangeChange: (PopularRange) -> Unit,
    showPopularTimeDropdown: Boolean,
    onVideoClick: (VideoSearchResult) -> Unit
) {
    if (videos.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        var isDropdownOpen by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = showPopularTimeDropdown) { isDropdownOpen = true }
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.featured,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (showPopularTimeDropdown) {
                    Text(
                        text = " ▾",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            DropdownMenu(
                expanded = isDropdownOpen && showPopularTimeDropdown,
                onDismissRequest = { isDropdownOpen = false }
            ) {
                PopularRange.values().forEach { range ->
                    val label = when (range) {
                        PopularRange.WEEK -> Strings.thisWeek
                        PopularRange.MONTH -> Strings.thisMonth
                        PopularRange.YEAR -> Strings.thisYear
                    }
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            isDropdownOpen = false
                            onPopularRangeChange(range)
                        },
                        trailingIcon = {
                            if (range == popularRange) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = videos.size.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val pagerState = rememberPagerState(initialPage = 0) { videos.size }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            pageSpacing = 12.dp,
            pageSize = PageSize.Fixed(240.dp)
        ) { page ->
            val video = videos[page]
            FeaturedCompactCard(
                video = video,
                onClick = { onVideoClick(video) }
            )
        }

        if (videos.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(videos.size) { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isActive) 7.dp else 6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedCompactCard(
    video: VideoSearchResult,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbCandidates = remember(video.videoId, video.thumbnail) {
        ThumbnailHelper.buildThumbnailCandidates(video.videoId, video.thumbnail)
    }
    var thumbIndex by remember(video.videoId, video.thumbnail) { mutableIntStateOf(0) }
    val thumbUrl = thumbCandidates.getOrElse(thumbIndex) { thumbCandidates.lastOrNull().orEmpty() }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(124.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbUrl)
                    .size(440, 248)
                    .crossfade(false)
                    .build(),
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    if (thumbIndex < thumbCandidates.lastIndex) {
                        thumbIndex += 1
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = video.channelName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (video.totalRatings > 0) {
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${video.totalRatings}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// Section divider with title
@Composable
internal fun SectionDivider(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
internal fun HomeBrowseHeaderCard(
    featured: List<VideoSearchResult>,
    popularRange: PopularRange,
    onPopularRangeChange: (PopularRange) -> Unit,
    showPopularTimeDropdown: Boolean = true,
    onVideoClick: (VideoSearchResult) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (featured.isNotEmpty()) {
            FeaturedVideosSection(
                videos = featured,
                popularRange = popularRange,
                onPopularRangeChange = onPopularRangeChange,
                showPopularTimeDropdown = showPopularTimeDropdown,
                onVideoClick = onVideoClick
            )
        }
    }
}
