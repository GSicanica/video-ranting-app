package com.youtube.rating.ioscomposeapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.HomeCategory
import com.youtube.rating.shared.models.VideoSearchResult

@Composable
internal fun HomeScreen(
    api: RatingApiClient
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<UiError?>(null) }
    var categories by remember { mutableStateOf<List<HomeCategory>>(emptyList()) }
    var topVideos by remember { mutableStateOf<List<VideoSearchResult>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        runCatching {
            val cats = api.getHomeCategories(forceRefresh = false)
            val top = api.getTopVideos(type = "rated", limit = 15)
            categories = cats.categories
            topVideos = top.videos
        }.onFailure {
            error = UiError(it.message ?: "Unknown error")
        }
        isLoading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Home", style = MaterialTheme.typography.headlineSmall)

        if (isLoading) {
            CircularProgressIndicator()
            return@Column
        }

        error?.let { Text("Greška: ${it.message}", color = MaterialTheme.colorScheme.error) }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (categories.isNotEmpty()) {
                item {
                    Text("Kategorije", style = MaterialTheme.typography.titleMedium)
                }
                items(categories, key = { it.id }) { cat ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            text = cat.name,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp)) }
            }

            if (topVideos.isNotEmpty()) {
                item {
                    Text("Top videi", style = MaterialTheme.typography.titleMedium)
                }
                items(topVideos, key = { it.videoId }) { v ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(v.title, style = MaterialTheme.typography.bodyLarge)
                            Text(v.channelName, style = MaterialTheme.typography.bodySmall)
                            v.category?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(
                                "Ocjene: ${v.totalRatings} | Avg: ${v.avgTotal.toFixed1()}",
                                style = MaterialTheme.typography.bodySmall
                            )
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
