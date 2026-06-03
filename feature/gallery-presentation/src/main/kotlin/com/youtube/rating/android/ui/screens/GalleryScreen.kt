package com.youtube.rating.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import coil.compose.AsyncImage
import com.youtube.rating.core.designsystem.components.MediaThumbnail
import coil.request.ImageRequest
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.viewmodel.GalleryViewModel
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val galleryImages by viewModel.galleryImages.collectAsStateWithLifecycle()
    val serverImages by viewModel.serverImages.collectAsStateWithLifecycle()
    val pinnedImages by viewModel.pinnedImages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSyncingServer by viewModel.isSyncingServer.collectAsStateWithLifecycle()
    val lastServerSyncAt by viewModel.lastServerSyncAt.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }

    // fullscreen viewer (čuvamo URI, ne index — sigurnije ako se lista promijeni)
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var editingImageUri by remember { mutableStateOf<String?>(null) }

    // ✅ Multi-select picker (bez permission gnjavaže)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        viewModel.addImagesFromDevice(context, uris) { added, failed ->
            if (failed > 0) Logger.error("GalleryScreen", "Failed to add $failed images")
            scope.launch {
                snackbarHostState.showSnackbar(
                    Strings.imagesAddedSummary(added = added, failed = failed)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadImages(context)
    }

    // snack za error (ako želiš)
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            HeaderCard(
                total = galleryImages.size,
                serverCount = serverImages.size,
                localCount = galleryImages.count { !(it.startsWith("http://") || it.startsWith("https://")) },
                pinned = pinnedImages.size,
                isLoading = isLoading,
                isSyncingServer = isSyncingServer,
                lastServerSyncAt = lastServerSyncAt,
                onRefreshServer = { viewModel.refreshServer(context) },
                onAddClick = { showAddDialog = true }
            )

            if (galleryImages.isEmpty() && !isLoading) {
                EmptyState(
                    onRefreshServer = { viewModel.refreshServer(context) },
                    isSyncingServer = isSyncingServer
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = galleryImages,
                        key = { it }
                    ) { imageUri ->
                        val isPinned = pinnedImages.contains(imageUri)
                        val isRemote = imageUri.startsWith("http://") || imageUri.startsWith("https://")
                        val canDelete = !isRemote
                        GalleryImageCard(
                            imageUri = imageUri,
                            isPinned = isPinned,
                            canDelete = canDelete,
                            isRemote = isRemote,
                            onClick = { selectedImageUri = imageUri },
                            onTogglePin = { viewModel.togglePin(context, imageUri) },
                            onDelete = { showDeleteConfirmDialog = imageUri }
                        )
                    }
                }
            }
        }
    }

    // ✅ Add image dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(Strings.addImagesTitle) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(Strings.selectFromGallery) },
                        supportingContent = { Text(Strings.multipleImagesInfo) },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showAddDialog = false
                            imagePickerLauncher.launch("image/*")
                        }
                    )
                    ListItem(
                        headlineContent = { Text(Strings.refreshFromServer) },
                        supportingContent = { Text(Strings.fetchLatestImagesInfo) },
                        leadingContent = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showAddDialog = false
                            viewModel.refreshServer(context)
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

    // ✅ Delete confirm
    showDeleteConfirmDialog?.let { imageUri ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text(Strings.deleteImage) },
            text = { Text(Strings.deleteImageWarning) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteImage(context, imageUri)
                        showDeleteConfirmDialog = null
                        if (selectedImageUri == imageUri) selectedImageUri = null
                    }
                ) { Text(Strings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text(Strings.cancel) }
            }
        )
    }

    // ✅ Fullscreen viewer (swipe + zoom + pin + delete)
    selectedImageUri?.let { uri ->
        val startIndex = remember(uri, galleryImages) {
            galleryImages.indexOf(uri).coerceAtLeast(0)
        }
        FullscreenGalleryViewer(
            images = galleryImages,
            pinned = pinnedImages.toSet(),
            startIndex = startIndex,
            onClose = { selectedImageUri = null },
            onTogglePin = { img -> viewModel.togglePin(context, img) },
            onRequestDelete = { img ->
                selectedImageUri = null
                showDeleteConfirmDialog = img
            },
            onRequestEdit = { img -> editingImageUri = img }
        )
    }

    editingImageUri?.let { imageUri ->
        ImageAnnotationEditorDialog(
            imageUri = imageUri,
            onDismiss = { editingImageUri = null },
            onSave = { annotatedBitmap ->
                viewModel.saveAnnotatedImage(
                    context = context,
                    sourceImageUri = imageUri,
                    annotatedBitmap = annotatedBitmap
                ) { savedPath ->
                    editingImageUri = null
                    if (savedPath != null) {
                        selectedImageUri = savedPath
                    }
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (savedPath != null) Strings.imageSavedSuccess else Strings.imageSaveFailed
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun HeaderCard(
    total: Int,
    serverCount: Int,
    localCount: Int,
    pinned: Int,
    isLoading: Boolean,
    isSyncingServer: Boolean,
    lastServerSyncAt: Long?,
    onRefreshServer: () -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Strings.galleryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = Strings.galleryStats(total, serverCount, localCount, pinned),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                lastServerSyncAt?.let { ts ->
                    val time = try {
                        android.text.format.DateFormat.format("HH:mm:ss", Date(ts)).toString()
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        ts.toString()
                    }
                    Text(
                        text = Strings.lastSyncLabel(time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                if (isLoading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.7f))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefreshServer,
                    enabled = !isSyncingServer && !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = Strings.refreshServer,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                FilledTonalButton(onClick = onAddClick, enabled = !isLoading) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.add)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onRefreshServer: () -> Unit,
    isSyncingServer: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Strings.noImagesInGallery,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Strings.addImagesToSeeHere,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(onClick = onRefreshServer, enabled = !isSyncingServer) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isSyncingServer) "${Strings.loading}…" else Strings.refreshFromServer)
            }
        }
    }
}

@Composable
fun GalleryImageCard(
    imageUri: String,
    isPinned: Boolean,
    canDelete: Boolean,
    isRemote: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val resolvedUri = remember(imageUri) {
        if (imageUri.startsWith("/")) "file://$imageUri" else imageUri
    }

    val req = remember(context, resolvedUri) {
        ImageRequest.Builder(context)
            .data(resolvedUri)
            .crossfade(true)
            .build()
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            MediaThumbnail(
                data = req,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // pin (gore desno)
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = Strings.pin,
                    tint = if (isPinned) Color(0xFFFFD54F) else Color.White
                )
            }

            if (isRemote) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(Strings.serverLabel, color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }

            // delete (dolje desno)
            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = Strings.delete, tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullscreenGalleryViewer(
    images: List<String>,
    pinned: Set<String>,
    startIndex: Int,
    onClose: () -> Unit,
    onTogglePin: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onRequestEdit: (String) -> Unit
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, images.lastIndex)
    ) { images.size }

    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(imageUri = images[page])
            }

            val currentUri = images.getOrNull(pagerState.currentPage)
            val isPinned = currentUri != null && pinned.contains(currentUri)
            val isRemote = currentUri != null && (currentUri.startsWith("http://") || currentUri.startsWith("https://"))
            val canDelete = currentUri != null && !isRemote
            val canEdit = currentUri != null && !isRemote

            // top bar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = Strings.close, tint = Color.White)
                }

                Text(
                    text = "${pagerState.currentPage + 1}/${images.size}",
                    color = Color.White.copy(alpha = 0.85f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRemote) {
                        Row(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(Strings.serverLabel, color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(
                        onClick = { currentUri?.let(onTogglePin) },
                        enabled = currentUri != null
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = Strings.pin,
                            tint = if (isPinned) Color(0xFFFFD54F) else Color.White
                        )
                    }
                    IconButton(
                        onClick = { currentUri?.let(onRequestEdit) },
                        enabled = canEdit
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = Strings.edit,
                            tint = if (canEdit) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = { currentUri?.let(onRequestDelete) },
                        enabled = canDelete
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = Strings.delete,
                            tint = if (canDelete) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(imageUri: String) {
    val context = LocalContext.current
    val resolved = remember(imageUri) {
        if (imageUri.startsWith("/")) "file://$imageUri" else imageUri
    }

    // zoom state per page
    var scale by remember(imageUri) { mutableStateOf(1f) }
    var offsetX by remember(imageUri) { mutableStateOf(0f) }
    var offsetY by remember(imageUri) { mutableStateOf(0f) }

    val req = remember(context, resolved) {
        ImageRequest.Builder(context)
            .data(resolved)
            .crossfade(false)
            .build()
    }

    AsyncImage(
        model = req,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imageUri) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale

                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
    )
}
