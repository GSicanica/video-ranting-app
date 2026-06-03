package com.youtube.rating.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.core.designsystem.components.MediaThumbnail
import com.youtube.rating.core.designsystem.components.RatingEmptyState
import com.youtube.rating.core.designsystem.components.RatingScaffold
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import com.youtube.rating.core.presentation.format.DateFormatters
import com.youtube.rating.core.presentation.media.ThumbnailUrls
import com.youtube.rating.watchhistory.domain.WatchHistoryEntry
import com.youtube.rating.android.viewmodel.WatchHistoryViewModel
import com.youtube.rating.core.designsystem.theme.spacing
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.util.*
import android.net.Uri
import kotlinx.coroutines.delay

private data class WatchHistoryUiState(
    val showClearDialog: Boolean = false,
)

/**
 * Watch History Screen
 * Displays list of watched videos with ability to clear history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHistoryScreen(
    viewModel: WatchHistoryViewModel = koinViewModel(),
    onVideoClick: (String) -> Unit = {}
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    var uiState by remember { mutableStateOf(WatchHistoryUiState()) }

    RatingScaffold(
        topBar = {
            RatingTopAppBar(
                title = "Povijest gledanja",
                subtitle = history.takeIf { it.isNotEmpty() }?.let { "${it.size} videa" },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !isLoading && !isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, "Osvježi")
                    }

                    // Clear all button
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { uiState = uiState.copy(showClearDialog = true) }) {
                            Icon(Icons.Default.Delete, "Obriši sve")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading && history.isEmpty() -> {
                    // Initial loading state
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                history.isEmpty() -> {
                    RatingEmptyState(
                        title = "Nema gledanih videa",
                        body = "Videi koje pogledaš će se prikazivati ovdje.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    // History list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        // Refresh indicator
                        if (isRefreshing) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        items(
                            items = history,
                            key = { it.videoId }
                        ) { entry ->
                            WatchHistoryCard(
                                entry = entry,
                                onClick = { onVideoClick(entry.videoId) },
                                onDelete = { viewModel.removeVideo(entry.videoId) }
                            )
                        }
                    }
                }
            }

            // Error snackbar
            error?.let { errorMsg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(spacing.lg),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("U redu")
                        }
                    }
                ) {
                    Text(errorMsg)
                }
            }
        }
    }

    // Clear confirmation dialog
    if (uiState.showClearDialog) {
        AlertDialog(
            onDismissRequest = { uiState = uiState.copy(showClearDialog = false) },
            title = { Text("Obrisati povijest?") },
            text = { Text("Ova radnja uklanja sve lokalno spremljene gledane videe.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        uiState = uiState.copy(showClearDialog = false)
                    }
                ) {
                    Text("Obriši", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { uiState = uiState.copy(showClearDialog = false) }) {
                    Text("Odustani")
                }
            }
        )
    }
}

/**
 * Individual watch history card with swipe-to-delete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchHistoryCard(
    entry: WatchHistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isDismissed by remember { mutableStateOf(false) }
    val cardShape = MaterialTheme.shapes.medium
    val spacing = MaterialTheme.spacing
    val imageShape = RoundedCornerShape(10.dp)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                isDismissed = true
                true
            } else {
                false
            }
        }
    )

    // Handle deletion after animation
    LaunchedEffect(isDismissed) {
        if (isDismissed) {
            delay(300) // Wait for animation
            onDelete()
        }
    }

    AnimatedVisibility(
        visible = !isDismissed,
        exit = shrinkVertically(
            animationSpec = tween(300),
            shrinkTowards = Alignment.Top
        ) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                // Red background with delete icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Obriši",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            enableDismissFromStartToEnd = false, // Only allow swipe from right to left
            enableDismissFromEndToStart = true
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    val thumbnailModel = remember(entry.thumbnail, entry.videoId) {
                        val t = entry.thumbnail
                        val normalized = t.trim()
                        val isValid = normalized.isNotEmpty() &&
                            !normalized.equals("null", ignoreCase = true) &&
                            !normalized.equals("undefined", ignoreCase = true) &&
                            !normalized.equals("nil", ignoreCase = true)
                        when {
                            normalized.startsWith("content://") -> Uri.parse(normalized)
                            normalized.startsWith("http://") || normalized.startsWith("https://") -> normalized
                            normalized.startsWith("/") -> File(normalized)
                            isValid -> normalized
                            else -> ThumbnailUrls.youtubeMaxRes(entry.videoId)
                        }
                    }

                    MediaThumbnail(
                        data = thumbnailModel,
                        contentDescription = entry.title,
                        modifier = Modifier
                            .size(88.dp, 64.dp)
                            .clip(imageShape),
                        contentScale = ContentScale.Crop
                    )

                    // Video info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = entry.channelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Gledano: ${DateFormatters.formatDateTime(entry.viewedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
