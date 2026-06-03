package com.youtube.rating.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun MediaThumbnail(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val model = when (data) {
        is ImageRequest -> data
        else -> ImageRequest.Builder(context)
            .data(data)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        placeholder = null,
        error = null,
        fallback = null,
        contentScale = contentScale,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
