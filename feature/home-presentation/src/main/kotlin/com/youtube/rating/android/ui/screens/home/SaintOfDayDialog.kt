package com.youtube.rating.android.ui.screens.home.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.youtube.rating.shared.models.SaintOfDayResponse
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.withStyle

@Composable
fun SaintOfDayDialog(
    saint: SaintOfDayResponse,
    onDismiss: () -> Unit,
    onImageClick: (() -> Unit)? = null,
    onPrev: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onPrev != null) {
                    IconButton(onClick = onPrev) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prethodni")
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = saint.title ?: "Svetac dana",
                        // Manje slova + do dvije linije radi boljeg smještaja
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.titleMedium.fontSize * 1.1f
                    )
                    val dateLabel = saint.date ?: saint.fetchedAt?.take(10)
                    if (!dateLabel.isNullOrBlank()) {
                        val formattedDate = runCatching {
                            val parts = dateLabel.split("-")
                            if (parts.size == 3) {
                                "${parts[2]}.${parts[1]}.${parts[0]}"
                            } else {
                                dateLabel
                            }
                        }.getOrDefault(dateLabel)
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onNext != null) {
                    IconButton(onClick = onNext) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Sljedeci")
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Zatvori")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val image = saint.image
                if (!image.isNullOrBlank()) {
                    AsyncImage(
                        model = image,
                        contentDescription = saint.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .let { base ->
                                if (onImageClick != null) {
                                    base.clickable { onImageClick() }
                                } else {
                                    base
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                }
                val content = saint.content.orEmpty().trim()
                if (content.isNotBlank()) {
                    Text(text = content, style = MaterialTheme.typography.bodyMedium)
                }
                val sourceUrl = saint.url ?: saint.source
                if (!sourceUrl.isNullOrBlank()) {
                    val uriHandler = LocalUriHandler.current
                    Text(
                        text = "Izvor: $sourceUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.clickable { uriHandler.openUri(sourceUrl) }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {

        }
    )
}

@Composable
fun SaintOfDayFullTextDialog(
    saint: SaintOfDayResponse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = saint.title ?: "Svetac dana",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Zatvori")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val content = saint.content.orEmpty().trim()
                if (content.isNotBlank()) {
                    Text(text = content, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
        }
    )
}
