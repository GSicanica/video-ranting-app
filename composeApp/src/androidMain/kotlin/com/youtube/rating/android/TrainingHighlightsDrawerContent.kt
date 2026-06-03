package com.youtube.rating.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import com.youtube.rating.android.data.BibleBooks
import com.youtube.rating.android.data.prefs.BibleReaderPrefs

@Composable
internal fun TrainingHighlightsDrawerContent(
    onNavigateToChapter: (bookId: String, chapter: Int, language: String, lineIndex: Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val highlightTokens by BibleReaderPrefs.bibleHighlightsFlow(context)
        .collectAsStateWithLifecycle(lifecycle = lifecycle, initialValue = emptySet())
    val highlightTexts by BibleReaderPrefs.bibleHighlightTextsFlow(context)
        .collectAsStateWithLifecycle(lifecycle = lifecycle, initialValue = emptyMap())

    data class HighlightItem(
        val token: String,
        val language: String,
        val bookId: String,
        val bookName: String,
        val bookIndex: Int,
        val chapter: Int,
        val lineIndex: Int,
        val text: String
    )

    val items = remember(highlightTokens, highlightTexts) {
        highlightTokens
            .mapNotNull { token ->
                val withoutPrefix = token.removePrefix("bible:")
                val langAndRest = withoutPrefix.split(":", limit = 2)
                val (lang, rest) = if (langAndRest.size == 2 && langAndRest[0].length == 2) {
                    langAndRest[0] to langAndRest[1]
                } else {
                    "hr" to withoutPrefix
                }
                val colonIdx = rest.lastIndexOf(':')
                if (colonIdx < 0) return@mapNotNull null
                val lineIndex = rest.substring(colonIdx + 1).toIntOrNull()
                    ?: return@mapNotNull null
                val bookChapter = rest.substring(0, colonIdx)
                val slashIdx = bookChapter.indexOf('/')
                if (slashIdx < 0) return@mapNotNull null
                val bookId = bookChapter.substring(0, slashIdx)
                val chapter = bookChapter.substring(slashIdx + 1).toIntOrNull()
                    ?: return@mapNotNull null
                val bookIndex = BibleBooks.books.indexOfFirst { it.id == bookId }
                val bookName = BibleBooks.books.getOrNull(bookIndex)?.name ?: bookId
                val text = highlightTexts[token] ?: ""
                HighlightItem(token = token, language = lang, bookId = bookId, bookName = bookName, bookIndex = bookIndex, chapter = chapter, lineIndex = lineIndex, text = text)
            }
            .sortedWith(compareBy({ it.bookIndex }, { it.chapter }, { it.lineIndex }))
    }

    val grouped = remember(items) { items.groupBy { "${it.language}:${it.bookId}/${it.chapter}" } }
    val yellowBg = Color(0xFFFFF59D)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Označene rečenice",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider()

        if (items.isEmpty()) {
            Text(
                "Nema označenih rečenica",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                grouped.forEach { (key, highlightItems) ->
                    val first = highlightItems.first()
                    item(key = "header_$key") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            onClick = { onNavigateToChapter(first.bookId, first.chapter, first.language, first.lineIndex) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${first.bookName} – Poglavlje ${first.chapter}",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    items(
                        highlightItems,
                        key = { it.token }
                    ) { item ->
                        val label = item.text.ifBlank { "Označeni redak ${item.lineIndex + 1}" }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            color = if (item.text.isNotBlank()) yellowBg.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            onClick = { onNavigateToChapter(item.bookId, item.chapter, item.language, item.lineIndex) }
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
