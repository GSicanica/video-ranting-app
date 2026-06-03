package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youtube.rating.core.designsystem.components.MediaThumbnail
import com.youtube.rating.core.designsystem.components.RatingCalloutCard
import com.youtube.rating.core.designsystem.components.RatingEmptyState
import com.youtube.rating.core.designsystem.components.RatingLoadingState
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.utils.AdminManager
import com.youtube.rating.core.designsystem.theme.spacing
import com.youtube.rating.android.viewmodel.RatedVideosViewModel
import com.youtube.rating.android.viewmodel.RatedVideosViewModelFactory
import com.youtube.rating.shared.models.RatedVideo
import org.koin.compose.koinInject
import com.youtube.rating.android.domain.usecase.ratings.BlockUserUseCase
import com.youtube.rating.android.domain.usecase.ratings.DeleteRatingUseCase
import com.youtube.rating.android.domain.usecase.ratings.UnblockUserUseCase
import com.youtube.rating.android.domain.usecase.ratings.UpdateRatingUseCase
import com.youtube.rating.android.utils.UserTokenManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.android.data.prefs.AdminPrefs
import com.youtube.rating.android.util.DateFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatedVideosScreen(
    adminManager: AdminManager = koinInject()
) {
    val context = LocalContext.current
    val userTokenManager: UserTokenManager = koinInject()
    val updateRatingUseCase: UpdateRatingUseCase = koinInject()
    val deleteRatingUseCase: DeleteRatingUseCase = koinInject()
    val blockUserUseCase: BlockUserUseCase = koinInject()
    val unblockUserUseCase: UnblockUserUseCase = koinInject()

    val viewModel: RatedVideosViewModel = viewModel(
        factory = RatedVideosViewModelFactory(
            updateRatingUseCase,
            deleteRatingUseCase,
            blockUserUseCase,
            unblockUserUseCase,
            userTokenManager
        )
    )

    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var editingVideo by remember { mutableStateOf<RatedVideo?>(null) }
    var deletingVideoId by remember { mutableStateOf<String?>(null) }

    val isAdminMode by AdminPrefs.adminModeFlow(context)
        .collectAsStateWithLifecycle(initialValue = adminManager.isAdminMode())
    val spacing = MaterialTheme.spacing

    LaunchedEffect(Unit) {
        viewModel.loadVideos(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    Strings.myRatingsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!isLoading && videos.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            "${videos.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> {
                    RatingLoadingState(
                        title = Strings.loading,
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center),
                    )
                }
                error != null -> {
                    RatingCalloutCard(
                        icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        title = Strings.error,
                        body = error.orEmpty(),
                        ctaLabel = Strings.tryAgain,
                        onCta = { viewModel.loadVideos(context) },
                        toneSurfaceVariant = false,
                        modifier = Modifier
                            .padding(spacing.xl)
                            .align(Alignment.Center),
                    )
                }
                videos.isEmpty() -> {
                    RatingEmptyState(
                        title = Strings.noRatedVideos,
                        body = Strings.ratedVideosEmptyMessage,
                        modifier = Modifier
                            .padding(spacing.xl)
                            .align(Alignment.Center),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        items(
                            items = videos,
                            key = { it.videoId },
                            contentType = { "rated_video" }
                        ) { video ->
                            RatedVideoCard(
                                video = video,
                                onEdit = { editingVideo = video },
                                onDelete = { deletingVideoId = video.videoId },
                                isAdminMode = isAdminMode
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit dialog
    editingVideo?.let { video ->
        EditRatingDialog(
            video = video,
            onDismiss = { editingVideo = null },
            onSave = { love, faith, hope ->
                viewModel.updateRating(video.videoId, love, faith, hope)
                editingVideo = null
            }
        )
    }

    // Delete confirmation dialog
    deletingVideoId?.let { videoId ->
        AlertDialog(
            onDismissRequest = { deletingVideoId = null },
            title = { Text(Strings.deleteRating) },
            text = { Text(Strings.deleteRatingWarning) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRating(videoId)
                        deletingVideoId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingVideoId = null }) {
                    Text(Strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun RatedVideoCard(
    video: RatedVideo,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isAdminMode: Boolean = false
) {
    val formattedDate = remember(video.ratedAt) { formatDate(video.ratedAt) }
    val spacing = MaterialTheme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(modifier = Modifier.padding(spacing.md)) {
            // Thumbnail
            MediaThumbnail(
                data = video.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(spacing.sm))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    video.videoTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    video.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.sm))

                // Ratings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RatingBadge(emoji = "❤️", value = video.myLove, color = Color(0xFFE91E63))
                    RatingBadge(emoji = "✝️", value = video.myFaith, color = Color(0xFF2196F3))
                    RatingBadge(emoji = "⭐", value = video.myHope, color = Color(0xFFFFC107))
                }

                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    "Ocjenjeno: $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Admin mode actions
            if (isAdminMode) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = Strings.editRating,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = Strings.deleteRating,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingBadge(emoji: String, value: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 12.sp)
            Text(
                value.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun EditRatingDialog(
    video: RatedVideo,
    onDismiss: () -> Unit,
    onSave: (love: Int, faith: Int, hope: Int) -> Unit
) {
    var love by remember { mutableStateOf(video.myLove.toFloat()) }
    var faith by remember { mutableStateOf(video.myFaith.toFloat()) }
    var hope by remember { mutableStateOf(video.myHope.toFloat()) }
    val spacing = MaterialTheme.spacing

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.editRating) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Text(
                    video.videoTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Love slider
                Column {
                    Text("${Strings.love} ❤️: ${love.toInt()}")
                    Slider(
                        value = love,
                        onValueChange = { love = it },
                        valueRange = 1f..3f,
                        steps = 1
                    )
                }

                // Faith slider
                Column {
                    Text("${Strings.faith} ✝️: ${faith.toInt()}")
                    Slider(
                        value = faith,
                        onValueChange = { faith = it },
                        valueRange = 1f..3f,
                        steps = 1
                    )
                }

                // Hope slider
                Column {
                    Text("${Strings.hope} ⭐: ${hope.toInt()}")
                    Slider(
                        value = hope,
                        onValueChange = { hope = it },
                        valueRange = 1f..3f,
                        steps = 1
                    )
                }

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(love.toInt(), faith.toInt(), hope.toInt())
                }
            ) {
                Text(Strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}

private fun formatDate(dateString: String): String {
    return DateFormatters.formatRatedAt(dateString)
}
