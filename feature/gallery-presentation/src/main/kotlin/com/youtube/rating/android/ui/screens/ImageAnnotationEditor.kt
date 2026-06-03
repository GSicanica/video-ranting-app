package com.youtube.rating.android.ui.screens

import com.youtube.rating.core.coroutines.ioDispatcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.youtube.rating.android.localization.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MarkerStroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float
)

data class TextOverlay(
    val text: String,
    val position: Offset,
    val color: Color,
    val sizePx: Float
)

private data class AnnotationSnapshot(
    val strokes: List<MarkerStroke>,
    val textOverlays: List<TextOverlay>
)

private const val MAX_HISTORY_SIZE = 50

@Composable
fun ImageAnnotationEditorDialog(
    imageUri: String,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var baseBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(imageUri) { mutableStateOf(true) }

    var currentSnapshot by remember(imageUri) {
        mutableStateOf(AnnotationSnapshot(strokes = emptyList(), textOverlays = emptyList()))
    }
    val undoStack = remember(imageUri) { mutableStateListOf<AnnotationSnapshot>() }
    val redoStack = remember(imageUri) { mutableStateListOf<AnnotationSnapshot>() }
    var currentStrokePoints by remember(imageUri) { mutableStateOf(emptyList<Offset>()) }

    var markerColor by remember(imageUri) { mutableStateOf(Color.Red) }
    var markerWidth by remember(imageUri) { mutableStateOf(10f) }
    var pendingText by remember(imageUri) { mutableStateOf("") }
    var placeTextMode by remember(imageUri) { mutableStateOf(false) }
    var canvasSize by remember(imageUri) { mutableStateOf(IntSize.Zero) }

    val canUndo = undoStack.isNotEmpty()
    val canRedo = redoStack.isNotEmpty()

    fun commitSnapshot(next: AnnotationSnapshot) {
        if (next == currentSnapshot) return
        undoStack.add(currentSnapshot)
        if (undoStack.size > MAX_HISTORY_SIZE) undoStack.removeAt(0)
        currentSnapshot = next
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(currentSnapshot)
        currentSnapshot = undoStack.removeAt(undoStack.lastIndex)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(currentSnapshot)
        if (undoStack.size > MAX_HISTORY_SIZE) undoStack.removeAt(0)
        currentSnapshot = redoStack.removeAt(redoStack.lastIndex)
    }

    LaunchedEffect(imageUri) {
        isLoading = true
        baseBitmap = withContext(ioDispatcher) {
            loadBitmap(context = context, imageUri = imageUri)
        }
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = Strings.loadingImage)
                }
            } else {
                val bitmap = baseBitmap
                if (bitmap == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = Strings.imageNotAvailable)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = Strings.closeEditor)
                            }
                            Text(
                                text = Strings.imageEditorTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalIconButton(
                                    onClick = { undo() },
                                    enabled = canUndo,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = Strings.undo)
                                }
                                Spacer(modifier = Modifier.size(6.dp))
                                FilledTonalIconButton(
                                    onClick = { redo() },
                                    enabled = canRedo,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = Strings.redo)
                                }
                                Spacer(modifier = Modifier.size(6.dp))
                                IconButton(
                                    onClick = {
                                        val mergedBitmap = mergeAnnotationsIntoBitmap(
                                            baseBitmap = bitmap,
                                            strokes = currentSnapshot.strokes,
                                            textOverlays = currentSnapshot.textOverlays
                                        )
                                        onSave(mergedBitmap)
                                    }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = Strings.saveImage)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black)
                                .onSizeChanged { canvasSize = it }
                                .pointerInput(placeTextMode, pendingText, markerColor) {
                                    detectTapGestures { tap ->
                                        if (!placeTextMode || pendingText.isBlank()) return@detectTapGestures

                                        val imageRect = computeDisplayedImageRect(
                                            containerSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                                            imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                                        )
                                        val imagePoint = toImageCoordinates(
                                            displayPoint = tap,
                                            imageRect = imageRect,
                                            imageWidth = bitmap.width,
                                            imageHeight = bitmap.height
                                        ) ?: return@detectTapGestures

                                        commitSnapshot(
                                            currentSnapshot.copy(
                                                textOverlays = currentSnapshot.textOverlays + TextOverlay(
                                                    text = pendingText.trim(),
                                                    position = imagePoint,
                                                    color = markerColor,
                                                    sizePx = 42f
                                                )
                                            )
                                        )
                                        pendingText = ""
                                        placeTextMode = false
                                    }
                                }
                                .pointerInput(placeTextMode, markerColor, markerWidth) {
                                    if (placeTextMode) return@pointerInput

                                    detectDragGestures(
                                        onDragStart = { startOffset ->
                                            val imageRect = computeDisplayedImageRect(
                                                containerSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                                                imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                                            )
                                            val imagePoint = toImageCoordinates(
                                                displayPoint = startOffset,
                                                imageRect = imageRect,
                                                imageWidth = bitmap.width,
                                                imageHeight = bitmap.height
                                            )
                                            currentStrokePoints = if (imagePoint != null) listOf(imagePoint) else emptyList()
                                        },
                                        onDragEnd = {
                                            if (currentStrokePoints.size > 1) {
                                                commitSnapshot(
                                                    currentSnapshot.copy(
                                                        strokes = currentSnapshot.strokes + MarkerStroke(
                                                            points = currentStrokePoints,
                                                            color = markerColor,
                                                            widthPx = markerWidth
                                                        )
                                                    )
                                                )
                                            }
                                            currentStrokePoints = emptyList()
                                        },
                                        onDragCancel = {
                                            currentStrokePoints = emptyList()
                                        }
                                    ) { change, _ ->
                                        val imageRect = computeDisplayedImageRect(
                                            containerSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                                            imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                                        )
                                        val imagePoint = toImageCoordinates(
                                            displayPoint = change.position,
                                            imageRect = imageRect,
                                            imageWidth = bitmap.width,
                                            imageHeight = bitmap.height
                                        )
                                        if (imagePoint != null) {
                                            currentStrokePoints = currentStrokePoints + imagePoint
                                            change.consume()
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val imageRect = computeDisplayedImageRect(
                                    containerSize = size,
                                    imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                                )

                                drawImage(
                                    image = bitmap.asImageBitmap(),
                                    dstOffset = IntOffset(imageRect.left.toInt(), imageRect.top.toInt()),
                                    dstSize = IntSize(imageRect.width.toInt(), imageRect.height.toInt())
                                )

                                val allStrokes = if (currentStrokePoints.size > 1) {
                                    currentSnapshot.strokes + MarkerStroke(currentStrokePoints, markerColor, markerWidth)
                                } else {
                                    currentSnapshot.strokes
                                }

                                allStrokes.forEach { stroke ->
                                    drawPreviewStroke(
                                        stroke = stroke,
                                        imageRect = imageRect,
                                        imageWidth = bitmap.width,
                                        imageHeight = bitmap.height
                                    )
                                }

                                currentSnapshot.textOverlays.forEach { textOverlay ->
                                    val displayPos = toDisplayCoordinates(
                                        imagePoint = textOverlay.position,
                                        imageRect = imageRect,
                                        imageWidth = bitmap.width,
                                        imageHeight = bitmap.height
                                    )
                                    drawContext.canvas.nativeCanvas.drawText(
                                        textOverlay.text,
                                        displayPos.x,
                                        displayPos.y,
                                        Paint().apply {
                                            isAntiAlias = true
                                            color = textOverlay.color.toArgb()
                                            textSize = textOverlay.sizePx
                                            style = Paint.Style.FILL
                                        }
                                    )
                                }
                            }

                            if (placeTextMode) {
                                Text(
                                    text = Strings.tapToPlaceText,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 12.dp)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = Strings.markerSize,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = markerWidth,
                                onValueChange = { markerWidth = it },
                                valueRange = 4f..32f
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                annotationColors().forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(color = color, shape = CircleShape)
                                            .border(
                                                width = if (color == markerColor) 2.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                shape = CircleShape
                                            )
                                            .clickable { markerColor = color }
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    commitSnapshot(AnnotationSnapshot(strokes = emptyList(), textOverlays = emptyList()))
                                    currentStrokePoints = emptyList()
                                }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = Strings.clearAnnotations)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = pendingText,
                                onValueChange = { pendingText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(Strings.textLabel) },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { placeTextMode = pendingText.isNotBlank() },
                                enabled = pendingText.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(Strings.placeText)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun annotationColors(): List<Color> {
    return listOf(
        Color.Red,
        Color(0xFFFFC107),
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color.White
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewStroke(
    stroke: MarkerStroke,
    imageRect: Rect,
    imageWidth: Int,
    imageHeight: Int
) {
    if (stroke.points.size < 2) return

    val path = androidx.compose.ui.graphics.Path()
    stroke.points.forEachIndexed { index, imagePoint ->
        val displayPoint = toDisplayCoordinates(
            imagePoint = imagePoint,
            imageRect = imageRect,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
        if (index == 0) {
            path.moveTo(displayPoint.x, displayPoint.y)
        } else {
            path.lineTo(displayPoint.x, displayPoint.y)
        }
    }

    drawPath(
        path = path,
        color = stroke.color,
        style = Stroke(width = stroke.widthPx, pathEffect = PathEffect.cornerPathEffect(12f))
    )
}

private fun computeDisplayedImageRect(containerSize: Size, imageSize: Size): Rect {
    if (containerSize.width <= 0f || containerSize.height <= 0f || imageSize.width <= 0f || imageSize.height <= 0f) {
        return Rect.Zero
    }
    val scale = minOf(containerSize.width / imageSize.width, containerSize.height / imageSize.height)
    val displayedWidth = imageSize.width * scale
    val displayedHeight = imageSize.height * scale
    val left = (containerSize.width - displayedWidth) / 2f
    val top = (containerSize.height - displayedHeight) / 2f
    return Rect(left = left, top = top, right = left + displayedWidth, bottom = top + displayedHeight)
}

private fun toImageCoordinates(
    displayPoint: Offset,
    imageRect: Rect,
    imageWidth: Int,
    imageHeight: Int
): Offset? {
    if (!imageRect.contains(displayPoint)) return null

    val normalizedX = (displayPoint.x - imageRect.left) / imageRect.width
    val normalizedY = (displayPoint.y - imageRect.top) / imageRect.height

    return Offset(
        x = normalizedX * imageWidth,
        y = normalizedY * imageHeight
    )
}

private fun toDisplayCoordinates(
    imagePoint: Offset,
    imageRect: Rect,
    imageWidth: Int,
    imageHeight: Int
): Offset {
    val normalizedX = imagePoint.x / imageWidth
    val normalizedY = imagePoint.y / imageHeight

    return Offset(
        x = imageRect.left + normalizedX * imageRect.width,
        y = imageRect.top + normalizedY * imageRect.height
    )
}

private fun mergeAnnotationsIntoBitmap(
    baseBitmap: Bitmap,
    strokes: List<MarkerStroke>,
    textOverlays: List<TextOverlay>
): Bitmap {
    val output = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)

    strokes.forEach { stroke ->
        if (stroke.points.size < 2) return@forEach
        val path = Path().apply {
            stroke.points.forEachIndexed { index, point ->
                if (index == 0) {
                    moveTo(point.x, point.y)
                } else {
                    lineTo(point.x, point.y)
                }
            }
        }
        val paint = Paint().apply {
            isAntiAlias = true
            color = stroke.color.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = stroke.widthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, paint)
    }

    textOverlays.forEach { textOverlay ->
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = textOverlay.color.toArgb()
            textSize = textOverlay.sizePx
            style = Paint.Style.FILL
        }
        canvas.drawText(textOverlay.text, textOverlay.position.x, textOverlay.position.y, textPaint)
    }

    return output
}

private fun loadBitmap(context: android.content.Context, imageUri: String): Bitmap? {
    return runCatching {
        when {
            imageUri.startsWith("/") -> BitmapFactory.decodeFile(imageUri)
            imageUri.startsWith("file://") -> BitmapFactory.decodeFile(Uri.parse(imageUri).path)
            imageUri.startsWith("content://") -> {
                val uri = Uri.parse(imageUri)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
            imageUri.startsWith("http://") || imageUri.startsWith("https://") -> null
            else -> BitmapFactory.decodeFile(imageUri)
        }
    }.getOrNull()
}





