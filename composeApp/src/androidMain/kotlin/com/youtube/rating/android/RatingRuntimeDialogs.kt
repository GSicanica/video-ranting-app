package com.youtube.rating.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.ContentLanguageManager
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.OfflineVideo
import com.youtube.rating.android.ui.components.OfflineVideoPlayerDialog

@Composable
internal fun RatingRuntimeDialogs(
    showContentLanguageDialog: Boolean,
    selectedContentLanguages: Set<Strings.Language>,
    contentLanguageManager: ContentLanguageManager,
    onContentLanguagesSelected: (Set<Strings.Language>) -> Unit,
    onDismissContentLanguageDialog: () -> Unit,
    showOfflineVideoPlayer: Boolean,
    selectedOfflineVideo: OfflineVideo?,
    onDismissOfflineVideo: () -> Unit
) {
    if (showContentLanguageDialog) {
        ContentLanguageSelectionDialog(
            selectedLanguages = selectedContentLanguages,
            contentLanguageManager = contentLanguageManager,
            onLanguagesSelected = onContentLanguagesSelected,
            onDismiss = onDismissContentLanguageDialog
        )
    }

    if (showOfflineVideoPlayer && selectedOfflineVideo != null) {
        OfflineVideoPlayerDialog(
            video = selectedOfflineVideo,
            onDismiss = onDismissOfflineVideo
        )
    }
}

@Composable
private fun ContentLanguageSelectionDialog(
    selectedLanguages: Set<Strings.Language>,
    contentLanguageManager: ContentLanguageManager,
    onLanguagesSelected: (Set<Strings.Language>) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedLanguages) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.contentLanguages) },
        text = {
            Column {
                Text(
                    text = Strings.selectContentLanguages,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Strings.Language.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSelected.contains(language),
                            onCheckedChange = { checked ->
                                tempSelected = if (checked) {
                                    tempSelected + language
                                } else {
                                    // Prevent deselecting all languages
                                    if (tempSelected.size > 1) {
                                        tempSelected - language
                                    } else {
                                        tempSelected
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = contentLanguageManager.getLanguageName(language),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onLanguagesSelected(tempSelected) }) {
                Text(Strings.apply)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}
