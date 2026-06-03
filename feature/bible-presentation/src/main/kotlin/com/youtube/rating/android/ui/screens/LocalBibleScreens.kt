package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.presentation.state.ResultState
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.android.viewmodel.LocalBibleBookViewModel
import com.youtube.rating.android.viewmodel.LocalBibleChapterViewModel
import com.youtube.rating.android.viewmodel.LocalBibleLibraryViewModel
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun LocalBibleLibraryScreen(
    onOpenBook: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LocalBibleLibraryViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val result by viewModel.result.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load(context)
    }

    Scaffold(
        topBar = {
            RatingTopAppBar(
                title = { Text(Strings.bible) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val r = result) {
                is ResultState.Error -> {
                    Text(
                        text = r.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ResultState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ResultState.Success -> {
                    val books = remember(r.data) { r.data.sortedBy { formatBookTitle(slug = it) } }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        books.forEach { book ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenBook(book) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = formatBookTitle(slug = book),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = book,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalBibleBookScreen(
    bookId: String,
    onOpenChapter: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LocalBibleBookViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val safeBook = remember(bookId) { bookId.removePrefix("22output1/").removePrefix("/") }
    val result by viewModel.result.collectAsStateWithLifecycle()
    val isPsalms = remember(safeBook) { safeBook.contains("psalm", ignoreCase = true) }

    LaunchedEffect(safeBook) {
        viewModel.load(context, safeBook)
    }

    Scaffold(
        topBar = {
            RatingTopAppBar(
                title = {
                    Text(
                        text = formatBookTitle(slug = bookId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val r = result) {
                is ResultState.Error -> {
                    Text(
                        text = r.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ResultState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ResultState.Success -> {
                    val chapters = remember(r.data) {
                        r.data.sortedWith(compareBy { chapterNumberFromFile(fileName = it) ?: Int.MAX_VALUE })
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isPsalms && chapters.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = Strings.quickPsalms,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val highlights = listOf(23, 51, 91, 103, 121)
                                    highlights.forEach { psalm ->
                                        val file = findChapterFile(chapters = chapters, target = psalm) ?: "$psalm.md"
                                        Card(
                                            modifier = Modifier,
                                            onClick = { onOpenChapter(file) }
                                        ) {
                                            Text(
                                                text = "P${psalm}",
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        chapters.forEach { chapter ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenChapter(chapter) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        val chapterLabel = chapterNumberFromFile(fileName = chapter)?.toString()
                                            ?: chapter.removeSuffix(".md")
                                        Text(
                                            text = Strings.chapterLabel(chapterLabel),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = chapter,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalBibleChapterScreen(
    bookId: String,
    chapterFile: String,
    onNavigateBack: () -> Unit,
    viewModel: LocalBibleChapterViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val safeBook = remember(bookId) { bookId.removePrefix("22output1/").removePrefix("/") }
    val safeChapter = remember(chapterFile) { chapterFile.substringAfterLast('/') }
    val result by viewModel.result.collectAsStateWithLifecycle()

    LaunchedEffect(safeBook, safeChapter) {
        viewModel.load(context, safeBook, safeChapter)
    }

    Scaffold(
        topBar = {
            RatingTopAppBar(
                title = {
                    val chapterLabel = chapterNumberFromFile(fileName = safeChapter)?.toString()
                        ?: safeChapter.removeSuffix(".md")
                    Text(
                        text = "${formatBookTitle(slug = safeBook)} $chapterLabel",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val r = result) {
                is ResultState.Error -> {
                    Text(
                        text = r.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ResultState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ResultState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        MarkdownText(
                            markdown = r.data,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

private fun formatBookTitle(slug: String): String {
    return slug
        .split('-')
        .joinToString(" ") { part ->
            if (part.isBlank()) part else part.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase() else ch.toString()
            }
        }
        .trim()
}

private fun chapterNumberFromFile(fileName: String): Int? {
    val base = fileName.removeSuffix(".md")
    return base.toIntOrNull()
}

private fun findChapterFile(chapters: List<String>, target: Int): String? {
    return chapters.firstOrNull { chapterNumberFromFile(fileName = it) == target }
}
