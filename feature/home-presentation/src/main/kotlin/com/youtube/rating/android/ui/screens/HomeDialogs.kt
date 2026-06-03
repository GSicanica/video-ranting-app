package com.youtube.rating.android.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.util.extractYouTubeVideoId
import com.youtube.rating.shared.models.VideoSearchResult

// -----------------------------------------------------------------------------
// Report dialog
// -----------------------------------------------------------------------------
@Composable
internal fun ReportVideoDialog(
    video: VideoSearchResult,
    isReporting: Boolean,
    error: String?,
    success: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isReporting) onDismiss() },
        title = { Text(Strings.reportVideoQuestion) },
        text = {
            Column {
                Text(Strings.reportVideoConfirmText)
                Spacer(modifier = Modifier.height(8.dp))
                Text(video.title, fontWeight = FontWeight.Normal, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Prijava će biti poslata administratoru na pregled.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                error?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            msg,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                success?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        )
                    ) {
                        Text(msg, modifier = Modifier.padding(12.dp), color = Color(0xFF2E7D32))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isReporting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(if (isReporting) "Prijavljujem..." else "Prijavi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isReporting) { Text(Strings.cancel) }
        }
    )
}

// -----------------------------------------------------------------------------
// URL input dialog
// -----------------------------------------------------------------------------
@Composable
internal fun UrlInputDialog(
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var dialogVideoUrl by rememberSaveable(initialUrl) { mutableStateOf(initialUrl) }
    val context = LocalContext.current

    val isMaybeValid = remember(dialogVideoUrl) {
        extractYouTubeVideoId(input = dialogVideoUrl) != null
    }

    AlertDialog(
        onDismissRequest = {
            dialogVideoUrl = ""
            onDismiss()
        },
        title = { Text(Strings.addVideo) },
        text = {
            Column {
                Text(Strings.enterYoutubeUrl, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = dialogVideoUrl,
                    onValueChange = { dialogVideoUrl = it },
                    placeholder = { Text("https://youtu.be/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val clipboardManager =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboardManager?.primaryClip
                                        ?.getItemAt(0)
                                        ?.coerceToText(context)
                                        ?.toString()
                                        ?.trim()
                                        .orEmpty()
                                    if (clip.isNotBlank()) dialogVideoUrl = clip
                                }
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Zalijepi")
                            }
                            if (dialogVideoUrl.isNotBlank()) {
                                IconButton(onClick = { dialogVideoUrl = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = Strings.clearSearch
                                    )
                                }
                            }
                        }
                    }
                )

                if (dialogVideoUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isMaybeValid) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isMaybeValid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isMaybeValid) Strings.validLink else Strings.validateYoutube,
                            color = if (isMaybeValid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(dialogVideoUrl.trim())
                    dialogVideoUrl = ""
                },
                enabled = isMaybeValid
            ) { Text(Strings.add) }
        },
        dismissButton = {
            TextButton(onClick = {
                dialogVideoUrl = ""
                onDismiss()
            }) { Text(Strings.cancel) }
        }
    )
}
