package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.Note
import com.youtube.rating.core.designsystem.components.RatingEmptyState
import com.youtube.rating.core.designsystem.components.RatingScaffold
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import com.youtube.rating.core.designsystem.theme.spacing
import com.youtube.rating.android.utils.rememberHapticFeedback
import com.youtube.rating.android.viewmodel.NotesViewModel
import com.youtube.rating.shared.utils.Logger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.youtube.rating.android.util.DateFormatters
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class NotesUiState(
    val showAddDialog: Boolean = false,
    val showEditDialog: Note? = null,
    val showDeleteDialog: Note? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val listState = rememberLazyListState()
    val spacing = MaterialTheme.spacing
    
    // ViewModel state
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    // UI state
    var uiState by remember { mutableStateOf(NotesUiState()) }
    
    // Load notes on first composition
    LaunchedEffect(Unit) {
        viewModel.loadNotes(context)
    }

    RatingScaffold(
        topBar = {
            RatingTopAppBar(
                title = Strings.myNotesTitle,
                subtitle = Strings.notesCount(notes.size),
                actions = {
                    IconButton(
                        onClick = {
                            haptic.lightTap()
                            uiState = uiState.copy(showAddDialog = true)
                        }
                    ) { Icon(Icons.Default.Add, contentDescription = Strings.add) }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.lightTap()
                    uiState = uiState.copy(showAddDialog = true)
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(Strings.add) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (notes.isEmpty()) {
                RatingEmptyState(
                    title = Strings.noNotes,
                    body = Strings.addFirstNote,
                    modifier = Modifier.align(Alignment.Center),
                    icon = {
                        Icon(
                            Icons.Default.NoteAlt,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    items(
                        items = notes,
                        key = { it.id },
                        contentType = { "note" }
                    ) { note ->
                        SimpleNoteCard(
                            note = note,
                            onEdit = {
                                haptic.lightTap()
                                uiState = uiState.copy(showEditDialog = note)
                            },
                            onDelete = {
                                haptic.strongFeedback()
                                uiState = uiState.copy(showDeleteDialog = note)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Note Dialog - Only content, no title
    if (uiState.showAddDialog) {
        SimpleNoteEditDialog(
            title = Strings.newNote,
            initialContent = "",
            onDismiss = { uiState = uiState.copy(showAddDialog = false) },
            onSave = { content ->
                // Use first line as title or generate one
                val title = content.lines().firstOrNull()?.take(50) ?: "Bilješka"
                viewModel.addNote(context, title, content) { success ->
                    if (success) {
                        Logger.ui("NotesScreen", "Added new note")
                        haptic.success()
                    }
                }
                uiState = uiState.copy(showAddDialog = false)
            }
        )
    }

    // Edit Note Dialog - Only content
    uiState.showEditDialog?.let { note ->
        SimpleNoteEditDialog(
            title = Strings.editNote,
            initialContent = note.content,
            onDismiss = { uiState = uiState.copy(showEditDialog = null) },
            onSave = { content ->
                val title = content.lines().firstOrNull()?.take(50) ?: "Bilješka"
                viewModel.updateNote(context, note.id, title, content) { success ->
                    if (success) {
                        Logger.ui("NotesScreen", "Updated note ${note.id}")
                        haptic.success()
                    }
                }
                uiState = uiState.copy(showEditDialog = null)
            }
        )
    }

    // Delete Confirmation Dialog
    uiState.showDeleteDialog?.let { note ->
        AlertDialog(
            onDismissRequest = { uiState = uiState.copy(showDeleteDialog = null) },
            title = { Text(Strings.deleteNoteTitle) },
            text = { Text(Strings.deleteNoteWarning(note.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(context, note.id) { success ->
                            if (success) {
                                haptic.success()
                            }
                        }
                        uiState = uiState.copy(showDeleteDialog = null)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { uiState = uiState.copy(showDeleteDialog = null) }) {
                    Text(Strings.cancel)
                }
            }
        )
    }
}

/**
 * Simple note card - only content, no title
 */
@Composable
fun SimpleNoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = Strings.delete,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatDate(note.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = Strings.edit,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = Strings.delete,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatDate(note.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Simple note edit dialog - only content field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleNoteEditDialog(
    title: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteContent by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text(Strings.contentLabel) },
                    placeholder = { Text(Strings.noteContentPlaceholder) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    maxLines = 15
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (noteContent.isNotBlank()) {
                                onSave(noteContent.trim())
                            }
                        },
                        enabled = noteContent.isNotBlank()
                    ) {
                        Text(Strings.save)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditDialog(
    title: String,
    initialTitle: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var noteTitle by remember { mutableStateOf(initialTitle) }
    var noteContent by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text(Strings.titleLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text(Strings.contentLabel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (noteTitle.isNotBlank() && noteContent.isNotBlank()) {
                                onSave(noteTitle, noteContent)
                            }
                        },
                        enabled = noteTitle.isNotBlank() && noteContent.isNotBlank()
                    ) {
                        Text(Strings.save)
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String =
    DateFormatters.formatDateTime(timestamp)
