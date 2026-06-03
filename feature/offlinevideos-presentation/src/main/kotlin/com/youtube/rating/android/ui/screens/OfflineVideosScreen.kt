// OfflineVideosScreen.kt (tvoj kod je već dobar; bitno je da “Galerija” button vodi na Gallery screen/tab)
package com.youtube.rating.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youtube.rating.core.designsystem.components.MediaThumbnail
import com.youtube.rating.core.designsystem.components.RatingScaffold
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.OfflineVideo
import com.youtube.rating.android.viewmodel.OfflineViewModel
import com.youtube.rating.shared.utils.Logger
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.core.designsystem.theme.spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineVideosScreen(
    onPlayVideo: (OfflineVideo) -> Unit = {},
    onOpenGallery: () -> Unit = {}, // ✅ ovo poveži sa “Gallery” screen/tab
    viewModel: OfflineViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val offlineVideos by viewModel.videos.collectAsStateWithLifecycle()
    val totalStorageUsed by viewModel.totalStorageUsed.collectAsStateWithLifecycle()

    LaunchedEffect(offlineVideos.size) {
        Logger.debug("OfflineVideosScreen", "Recomposing - videos count: ${offlineVideos.size}")
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<OfflineVideo?>(null) }
    var showEditDialog by remember { mutableStateOf<OfflineVideo?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addVideoFromDevice(context, it, "New Video", "General") { }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadVideos(context)
    }

    val totalStorage = remember(totalStorageUsed) {
        val mb = totalStorageUsed / (1024.0 * 1024.0)
        if (mb >= 1024) String.format("%.2f GB", mb / 1024.0) else String.format("%.1f MB", mb)
    }

    val spacing = MaterialTheme.spacing
    val subtitle = remember(offlineVideos.size, totalStorage) {
        "${offlineVideos.size} ${Strings.videos} • $totalStorage"
    }

    RatingScaffold(
        topBar = {
            RatingTopAppBar(
                title = Strings.offlineVideos,
                subtitle = subtitle,
                actions = {
                    IconButton(onClick = onOpenGallery) {
                        Icon(Icons.Default.Image, contentDescription = Strings.galleryTitle)
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = Strings.addVideo)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        Text(
                            text = Strings.offlineVideos,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                onClick = onOpenGallery
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.galleryTitle, maxLines = 1)
                            }

                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                onClick = { showAddDialog = true }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.addVideo, maxLines = 1)
                            }
                        }
                    }
                }
            }

            if (offlineVideos.isEmpty()) {
                item {
                    com.youtube.rating.core.designsystem.components.RatingEmptyState(
                        title = Strings.noOfflineVideos,
                        body = Strings.addVideosToWatchOffline,
                        icon = {
                            Icon(
                                Icons.Default.OfflinePin,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            } else {
                items(
                    items = offlineVideos,
                    key = { it.id },
                    contentType = { "offline_video" }
                ) { video ->
                    OfflineVideoCard(
                        video = video,
                        onClick = { onPlayVideo(video) },
                        onEdit = { showEditDialog = video },
                        onDelete = { showDeleteConfirmDialog = video }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(Strings.addVideo) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(Strings.chooseFromGallery) },
                        supportingContent = { Text(Strings.pickVideoFromDevice) },
                        leadingContent = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showAddDialog = false
                            videoPickerLauncher.launch("video/*")
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(Strings.cancel) }
            }
        )
    }

    showDeleteConfirmDialog?.let { video ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text(Strings.deleteVideo) },
            text = { Text(Strings.deleteVideoConfirmation(video.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVideo(context, video.id) { success ->
                            if (success) showDeleteConfirmDialog = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(Strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text(Strings.cancel) }
            }
        )
    }

    showEditDialog?.let { video ->
        var newTitle by remember { mutableStateOf(video.title) }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text(Strings.editVideo) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(Strings.videoTitle) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateVideo(context, video.id, newTitle, null) { success ->
                            if (success) showEditDialog = null
                        }
                    }
                ) { Text(Strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) { Text(Strings.cancel) }
            }
        )
    }

}

@Composable
fun OfflineVideoCard(
    video: OfflineVideo,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val thumbnail = remember(video.thumbnailPath, video.thumbnailUrl) { video.getThumbnail() }
    val thumbnailRequest = remember(context, thumbnail) {
        thumbnail?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp, 70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (thumbnail != null) {
                    MediaThumbnail(
                        data = thumbnailRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.VideoFile,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (video.duration > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = video.getFormattedDuration(),
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Icon(
                    Icons.Default.OfflinePin,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopStart)
                        .padding(2.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = video.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (video.isFromDevice) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (video.isFromDevice) Strings.localVideo else "YouTube",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = video.getFormattedFileSize(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(Strings.edit) },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.delete) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
