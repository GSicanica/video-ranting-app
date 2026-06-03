package com.youtube.rating.ioscomposeapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.youtube.rating.shared.models.BibleSearchResult
import kotlinx.coroutines.launch

@Composable
internal fun TrainingScreen(
    api: RatingApiClient
) {
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiError?>(null) }
    var results by remember { mutableStateOf<List<BibleSearchResult>>(emptyList()) }
    val scope = rememberCoroutineScope()

    suspend fun doSearch() {
        val q = query.trim()
        if (q.isBlank()) return
        isLoading = true
        error = null
        results = emptyList()
        runCatching {
            val resp = api.searchBible(query = q)
            if (!resp.success) {
                error = UiError(resp.message ?: "Server error")
            }
            results = resp.results
        }.onFailure {
            error = UiError(it.message ?: "Unknown error")
        }
        isLoading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Training", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search Bible") }
        )

        Button(
            onClick = { scope.launch { doSearch() } },
            enabled = !isLoading
        ) { Text("Search") }

        if (isLoading) CircularProgressIndicator()
        error?.let { Text("Error: ${it.message}", color = MaterialTheme.colorScheme.error) }

        if (!isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { "${it.book}-${it.chapter}-${it.snippet}" }) { item ->
                    Card {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("${item.book} ${item.chapter}", style = MaterialTheme.typography.titleSmall)
                            Text(item.snippet ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
