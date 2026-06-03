package com.youtube.rating.ioscomposeapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.VideoSearchResult
import kotlinx.coroutines.launch

@Composable
internal fun SearchScreen(
    api: RatingApiClient
) {
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiError?>(null) }
    var results by remember { mutableStateOf<List<VideoSearchResult>>(emptyList()) }
    val scope = rememberCoroutineScope()

    suspend fun doSearch() {
        val q = query.trim()
        if (q.isBlank()) return
        isLoading = true
        error = null
        results = emptyList()
        runCatching {
            val resp = api.searchVideosV2(searchQuery = q, perPage = 30, page = 1)
            results = resp.videos
        }.onFailure {
            error = UiError(it.message ?: "Unknown error")
        }
        isLoading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Search", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Upiši pojam") }
        )
        Button(
            onClick = { scope.launch { doSearch() } },
            enabled = !isLoading
        ) { Text("Traži") }

        if (isLoading) CircularProgressIndicator()
        error?.let { Text("Greška: ${it.message}", color = MaterialTheme.colorScheme.error) }

        if (!isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.videoId }) { v ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(v.title, style = MaterialTheme.typography.bodyLarge)
                            Text(v.channelName, style = MaterialTheme.typography.bodySmall)
                            Text("Ocjene: ${v.totalRatings} | Avg: ${v.avgTotal.toFixed1()}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun Double.toFixed1(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return rounded.toString()
}
