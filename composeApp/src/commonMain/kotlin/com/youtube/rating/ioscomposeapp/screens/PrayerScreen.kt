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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtube.rating.ioscomposeapp.platform.PlatformUserToken
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.data.PrayerRequestDto
import kotlinx.coroutines.launch

@Composable
internal fun PrayerScreen(
    api: RatingApiClient
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<UiError?>(null) }
    var items by remember { mutableStateOf<List<PrayerRequestDto>>(emptyList()) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        isLoading = true
        error = null
        runCatching {
            val token = PlatformUserToken.getOrCreate(api)
            val page = api.getPrayerRequests(userToken = token, page = 1, pageSize = 30)
            items = page.items
        }.onFailure {
            error = UiError(it.message ?: "Unknown error")
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Prayer", style = MaterialTheme.typography.headlineSmall)

        if (isLoading) {
            CircularProgressIndicator()
            return@Column
        }

        error?.let { Text("Error: ${it.message}", color = MaterialTheme.colorScheme.error) }
        Button(onClick = { scope.launch { load() } }) { Text("Refresh") }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Card {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(item.authorName ?: "Anonymous", style = MaterialTheme.typography.titleSmall)
                        Text(item.text, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Prayed: ${item.prayedCount}  Encouragements: ${item.encouragementCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
