package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.BibleSearchResult
import com.youtube.rating.shared.models.RandomPassageResponse
import com.youtube.rating.core.presentation.state.ResultState
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.youtube.rating.android.data.prefs.BibleReaderPrefs

private data class BibleSearchUiState(
    val query: String = "",
    val result: ResultState<List<BibleSearchResult>>? = null,
    val searched: Boolean = false,
    val selectedResult: BibleSearchResult? = null,
    val searchToken: Long = 0L,
)

private data class BibleSearchPassageUiState(
    val result: ResultState<RandomPassageResponse> = ResultState.Loading,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleSearchScreen(onDismiss: () -> Unit) {
    val apiClient: RatingApiClient = koinInject()
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf(BibleSearchUiState()) }

    fun runSearch() {
        val q = uiState.query.trim()
        if (q.length < 2) {
            uiState = uiState.copy(
                result = ResultState.Error("Upiši barem 2 slova."),
                searched = true
            )
            return
        }
        val token = uiState.searchToken + 1
        uiState = uiState.copy(
            searchToken = token,
            result = ResultState.Loading,
            searched = true
        )
        scope.launch {
            val response = runCatching { apiClient.searchBible(q, limit = 60) }.getOrNull()
            if (token != uiState.searchToken) return@launch
            if (response == null || !response.success) {
                uiState = uiState.copy(
                    result = ResultState.Error(response?.message ?: "Greška pri pretraživanju.")
                )
            } else {
                uiState = uiState.copy(
                    result = ResultState.Success(response.results)
                )
            }
        }
    }

    Scaffold(
        topBar = {
            RatingTopAppBar(
                title = Strings.bibleSearchTitle,
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Natrag")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { uiState = uiState.copy(query = it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { uiState = uiState.copy(query = "") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Očisti")
                        }
                    }
                },
                placeholder = { Text(Strings.bibleSearchPlaceholder) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() })
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = ::runSearch, modifier = Modifier.weight(1f)) {
                    Text(Strings.search)
                }
            }

            if (uiState.result is ResultState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            (uiState.result as? ResultState.Error)?.let { err ->
                Text(err.message, color = MaterialTheme.colorScheme.error)
            }

            val resultItems = (uiState.result as? ResultState.Success<List<BibleSearchResult>>)?.data.orEmpty()
            if (uiState.searched && uiState.result !is ResultState.Loading && resultItems.isEmpty() && uiState.result !is ResultState.Error) {
                Text(
                    "Nema rezultata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (resultItems.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(resultItems, key = { "${it.book}:${it.chapter}:${it.snippet}" }) { item ->
                        val bookTitle = formatBookTitle(slug = item.book)
                        val chapterLabel = formatChapterLabel(raw = item.chapter)
                        Card(
                            onClick = { uiState = uiState.copy(selectedResult = item) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "$bookTitle $chapterLabel",
                                    fontWeight = FontWeight.SemiBold
                                )
                                val snippet = item.snippet?.trim().orEmpty()
                                if (snippet.isNotBlank()) {
                                    Text(
                                        snippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    if (uiState.selectedResult != null) {
        Dialog(
            onDismissRequest = { uiState = uiState.copy(selectedResult = null) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BibleSearchPassageScreen(
                result = uiState.selectedResult,
                onDismiss = { uiState = uiState.copy(selectedResult = null) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BibleSearchPassageScreen(
    result: BibleSearchResult?,
    onDismiss: () -> Unit
) {
    if (result == null) return
    val apiClient: RatingApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val fontSp by BibleReaderPrefs.bibleReaderFontSpFlow(context)
        .collectAsStateWithLifecycle(lifecycle = lifecycle, initialValue = 14)

    var uiState by remember { mutableStateOf(BibleSearchPassageUiState()) }

    LaunchedEffect(result.book, result.chapter) {
        uiState = uiState.copy(result = ResultState.Loading)
        val response = runCatching {
            apiClient.getBiblePassage(result.book, result.chapter)
        }.getOrNull()
        if (response != null && response.success) {
            uiState = uiState.copy(result = ResultState.Success(response))
        } else {
            uiState = uiState.copy(result = ResultState.Error(response?.message ?: "Greška pri učitavanju."))
        }
    }

    val loadedPassage = (uiState.result as? ResultState.Success<RandomPassageResponse>)?.data
    val title = loadedPassage?.title ?: "${formatBookTitle(slug = result.book)} ${formatChapterLabel(raw = result.chapter)}"
    val displayFont = fontSp.coerceIn(10, 30)
    val lineHeightSp = (displayFont + 10).coerceIn(displayFont + 6, displayFont + 14)

    Scaffold(
        topBar = {
            RatingTopAppBar(
                title = {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Natrag")
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                val next = (displayFont - 1).coerceAtLeast(10)
                                scope.launch { BibleReaderPrefs.setBibleReaderFontSp(context, next) }
                            },
                            enabled = displayFont > 10
                        ) { Icon(Icons.Default.Remove, contentDescription = "Smanji") }
                        Text("${displayFont}sp", style = MaterialTheme.typography.labelMedium)
                        IconButton(
                            onClick = {
                                val next = (displayFont + 1).coerceAtMost(30)
                                scope.launch { BibleReaderPrefs.setBibleReaderFontSp(context, next) }
                            },
                            enabled = displayFont < 30
                        ) { Icon(Icons.Default.Add, contentDescription = "Povećaj") }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.result is ResultState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                uiState.result is ResultState.Error -> {
                    Text(
                        text = (uiState.result as? ResultState.Error)?.message ?: "Greška.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        MarkdownText(
                            markdown = loadedPassage?.text.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = displayFont.sp,
                                lineHeight = lineHeightSp.sp
                            )
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

private fun formatBookTitle(slug: String): String {
    val cleaned = slug
        .replace('_', '-')
        .replace('/', '-')
        .trim('-')
    return cleaned
        .split('-')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            if (part.isBlank()) part else part.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase() else ch.toString()
            }
        }
        .trim()
}

private fun formatChapterLabel(raw: String): String {
    val cleaned = raw.removeSuffix(".md").trim()
    val noZeros = cleaned.trimStart('0')
    return if (noZeros.isBlank()) cleaned else noZeros
}
