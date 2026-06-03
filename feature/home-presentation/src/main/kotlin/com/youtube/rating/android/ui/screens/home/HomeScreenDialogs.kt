package com.youtube.rating.android.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.ui.models.VideoCloseAction
import com.youtube.rating.android.ui.screens.home.dialogs.SaintOfDayDialog
import com.youtube.rating.android.ui.screens.home.dialogs.SaintOfDayFullTextDialog
import com.youtube.rating.shared.models.SaintOfDayResponse
import com.youtube.rating.shared.models.VideoSearchResult

@Composable
internal fun HomeDeleteVideoDialog(
    video: VideoSearchResult,
    loading: Boolean,
    error: String?,
    success: String?,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!loading) onDismiss()
        },
        title = { Text(Strings.deleteVideoConfirmTitle) },
        text = {
            Column {
                Text(Strings.deleteVideoConfirmText)
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                Text("\"${video.title}\"", fontWeight = FontWeight.Normal)
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                Text(
                    "Ova akcija će obrisati video, sve ocjene i komentare. Ne može se poništiti.",
                    color = MaterialTheme.colorScheme.error
                )

                error?.let {
                    Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                success?.let {
                    Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = androidx.compose.ui.Modifier.size(16.dp),
                        color = Color.White
                    )
                } else {
                    Text(Strings.delete)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !loading
            ) { Text(Strings.cancel) }
        }
    )
}

@Composable
internal fun HomeCloseChoiceDialog(
    isAdminMode: Boolean,
    onSelect: (VideoCloseAction) -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(Strings.continueWatchingTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Strings.continueWatchingText)
                OutlinedButton(
                    onClick = { onSelect(VideoCloseAction.MINI_PLAYER) },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                ) {
                    Text(Strings.miniPlayerBottom)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(Strings.cancel)
            }
        }
    )
}

@Composable
internal fun HomeSaintDialogs(
    saint: SaintOfDayResponse,
    showSaintDialog: Boolean,
    showSaintFullDialog: Boolean,
    onDismissSaint: () -> Unit,
    onImageClick: () -> Unit,
    onPrev: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onDismissFull: () -> Unit
) {
    if (showSaintDialog) {
        SaintOfDayDialog(
            saint = saint,
            onDismiss = onDismissSaint,
            onImageClick = onImageClick,
            onPrev = onPrev,
            onNext = onNext
        )
    }
    if (showSaintFullDialog) {
        SaintOfDayFullTextDialog(
            saint = saint,
            onDismiss = onDismissFull
        )
    }
}
