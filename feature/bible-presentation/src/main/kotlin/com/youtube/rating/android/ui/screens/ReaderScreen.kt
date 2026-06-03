package com.youtube.rating.android.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextAlign
import com.youtube.rating.android.data.BibleBooks
import com.youtube.rating.android.utils.BibleApiService
import java.time.LocalDate

private const val TYPE_BIBLE = "bible"
private const val TYPE_GOSPEL = "gospel"
private const val TYPE_LOCAL = "local"

@Composable
fun ReaderScreen(
    type: String,
    book: String?,
    chapter: String?,
    lang: String?,
    line: Int?,
    date: String?,
    onDismiss: () -> Unit,
    onOpenLeftDrawer: () -> Unit = {},
    onOpenRightDrawer: () -> Unit = {},
    gospelContent: @Composable (LocalDate?) -> Unit = {
        Text(
            text = "Gospel reader is not available",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
) {
    when (type) {
        TYPE_BIBLE -> {
            val bookId = book ?: "postanak"
            val chapterInt = chapter?.toIntOrNull() ?: 1
            val langCode = lang ?: "hr"
            val bookIndex = BibleBooks.books.indexOfFirst { it.id == bookId }.takeIf { it >= 0 } ?: 0
            val languageOverride = when (langCode.lowercase()) {
                "en" -> BibleApiService.BibleLanguage.ENGLISH
                "de" -> BibleApiService.BibleLanguage.GERMAN
                else -> BibleApiService.BibleLanguage.CROATIAN
            }
            SequentialBibleReaderScreen(
                initialBookIndex = bookIndex,
                initialChapter = chapterInt,
                languageOverride = languageOverride,
                initialHighlightLine = line?.takeIf { it >= 0 },
                showDrawerIcons = true,
                onProgressChanged = { _, _ -> },
                onChapterRead = { },
                onDismiss = onDismiss,
                onOpenLeftDrawer = onOpenLeftDrawer,
                onOpenRightDrawer = onOpenRightDrawer
            )
        }
        TYPE_LOCAL -> {
            val safeBook = book?.takeIf { it.isNotBlank() }
            val safeChapter = chapter?.takeIf { it.isNotBlank() }
            if (safeBook == null || safeChapter == null) {
                Text(
                    text = "Missing local reader arguments",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            } else {
                LocalBibleChapterScreen(
                    bookId = safeBook,
                    chapterFile = safeChapter,
                    onNavigateBack = onDismiss
                )
            }
        }
        TYPE_GOSPEL -> {
            val initialDate = remember(date) {
                runCatching { date?.let { LocalDate.parse(it) } }.getOrNull()
            }
            gospelContent(initialDate)
        }
        else -> {
            Text(
                text = "Unknown reader type",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
