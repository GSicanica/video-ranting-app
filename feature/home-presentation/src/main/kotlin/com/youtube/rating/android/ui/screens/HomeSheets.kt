package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.designsystem.components.BrowseControlsCard
import com.youtube.rating.core.designsystem.components.SimpleRatingSelector
import com.youtube.rating.android.ui.models.BrowseRatingFilters
import com.youtube.rating.android.ui.models.QuickRateDraft
import com.youtube.rating.android.viewmodel.RatingViewModel
import com.youtube.rating.shared.models.VideoSearchResult

// -----------------------------------------------------------------------------
// Browse controls sheet (filters/search/sort/view)
// -----------------------------------------------------------------------------
@Composable
internal fun BrowseControlsSheet(
    onSearchClick: () -> Unit,
    onOpenClips: () -> Unit,
    isBrowsing: Boolean,
    browseVideosCount: Int,
    totalResults: Int,
    isGridView: Boolean,
    sortBy: String?,
    onToggleView: (Boolean) -> Unit,
    onSortChange: (String?) -> Unit,
    ratingFilters: BrowseRatingFilters,
    onRatingFiltersChange: (BrowseRatingFilters) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val config = LocalConfiguration.current

    val shownCount by remember(browseVideosCount, totalResults) {
        derivedStateOf {
            if (totalResults > 0) totalResults else browseVideosCount
        }
    }
    val badgeText by remember(shownCount) {
        derivedStateOf { if (shownCount > 999) "999+" else shownCount.toString() }
    }

    val maxSheetHeight = remember(config.screenHeightDp) { config.screenHeightDp.dp * 0.92f }
    val bottomBarHeight = 76.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 0.dp,
                    bottom = bottomBarHeight + 4.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        // Rating filters
                        CompactRatingFiltersRow(
                            ratingFilters = ratingFilters,
                            onRatingFiltersChange = onRatingFiltersChange
                        )

                        // Sorting and view controls
                        BrowseControlsCard(
                            isBrowsing = isBrowsing,
                            browseVideosCount = browseVideosCount,
                            isGridView = isGridView,
                            sortBy = sortBy,
                            onToggleView = onToggleView,
                            onSortChange = onSortChange,
                            browseError = ""
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                tonalElevation = 10.dp,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                            onOpenClips()
                            onDismiss()
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(Strings.clips, style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                            onResetAll()
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(Strings.reset, style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                            onSearchClick()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        enabled = !isBrowsing
                    ) {
                        if (isBrowsing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(Strings.applying)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(if (shownCount > 0) "${Strings.apply} ($badgeText)" else Strings.apply)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Quick rate sheet
// -----------------------------------------------------------------------------
@Composable
internal fun QuickRateSheet(
    video: VideoSearchResult,
    draft: QuickRateDraft,
    submitState: RatingViewModel.SubmitState,
    sheetState: SheetState,
    onDraftChange: (QuickRateDraft) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (QuickRateDraft) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Brza ocjena",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal
            )
            Text(
                video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                video.channelName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SimpleRatingSelector(
                title = "❤️ Ljubav",
                rating = draft.love,
                selectedIcon = Icons.Filled.Favorite,
                unselectedIcon = Icons.Filled.FavoriteBorder,
                onRatingChange = { onDraftChange(draft.copy(love = it)) }
            )

            SimpleRatingSelector(
                title = "✝️ Vjera",
                rating = draft.faith,
                selectedIcon = Icons.Filled.Star,
                unselectedIcon = Icons.Outlined.StarBorder,
                onRatingChange = { onDraftChange(draft.copy(faith = it)) }
            )

            SimpleRatingSelector(
                title = "⭐ Nada",
                rating = draft.hope,
                selectedIcon = Icons.Filled.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome,
                onRatingChange = { onDraftChange(draft.copy(hope = it)) }
            )

            val canSend = draft.love > 0 && draft.faith > 0 && draft.hope > 0
            Button(
                onClick = { onSubmit(draft) },
                enabled = canSend && submitState !is RatingViewModel.SubmitState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                if (submitState is RatingViewModel.SubmitState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.sending)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.sendRating)
                }
            }
        }
    }
}

@Composable
internal fun QuickActionsSheet(
    video: VideoSearchResult,
    sheetState: SheetState,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Brzi meni",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal
            )
            Text(
                video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                video.channelName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    onFavoriteToggle()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isFavorite) Strings.removeFromFavorites else Strings.addToFavorites)
            }

            OutlinedButton(
                onClick = {
                    onShare()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(Strings.share)
            }

            OutlinedButton(
                onClick = {
                    onReport()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Error, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(Strings.report)
            }
        }
    }
}
