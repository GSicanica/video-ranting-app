package com.youtube.rating.ioscomposeapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.youtube.rating.ioscomposeapp.platform.PlatformUserToken
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.RatingRequest
import com.youtube.rating.shared.models.YouTubeVideoInfo
import kotlinx.coroutines.launch

@Composable
internal fun RateScreen(api: RatingApiClient) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiError?>(null) }
    var info by remember { mutableStateOf<YouTubeVideoInfo?>(null) }
    var love by remember { mutableStateOf("3") }
    var faith by remember { mutableStateOf("3") }
    var hope by remember { mutableStateOf("3") }
    var status by remember { mutableStateOf<String?>(null) }

    fun extractVideoId(raw: String): String? {
        val s = raw.trim()
        if (s.isBlank()) return null
        val idRegex = Regex("""(?:(?:v=)|(?:youtu\.be/)|(?:/shorts/))([A-Za-z0-9_-]{6,})""")
        return idRegex.find(s)?.groupValues?.getOrNull(1)
            ?: s.takeIf { it.matches(Regex("""[A-Za-z0-9_-]{6,}""")) }
    }

    fun toRating(value: String): Int? = value.trim().toIntOrNull()?.takeIf { it in 1..3 }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Rate", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("YouTube URL ili videoId") }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !isLoading, onClick = {
                val id = extractVideoId(input)
                if (id == null) {
                    error = UiError("Ne mogu prepoznati videoId.")
                    return@Button
                }
                scope.launch {
                    isLoading = true
                    error = null
                    status = null
                    info = null
                    runCatching { api.getYouTubeVideoInfo(videoId = id) }
                        .onSuccess { info = it }
                        .onFailure { error = UiError(it.message ?: "Unknown error") }
                    isLoading = false
                }
            }) { Text("Učitaj") }

            Button(enabled = !isLoading && info != null, onClick = {
                val loaded = info ?: return@Button
                val l = toRating(love)
                val f = toRating(faith)
                val h = toRating(hope)
                if (l == null || f == null || h == null) {
                    error = UiError("Ocjene moraju biti 1–3.")
                    return@Button
                }
                scope.launch {
                    isLoading = true
                    error = null
                    status = null
                    runCatching {
                        val token = PlatformUserToken.getOrCreate(api)
                        val req = RatingRequest(
                            videoId = loaded.videoId,
                            videoTitle = loaded.title,
                            videoThumbnail = loaded.thumbnail,
                            videoChannel = loaded.channelName,
                            love = l,
                            faith = f,
                            hope = h,
                            category = null,
                            userToken = token,
                            language = loaded.language
                        )
                        api.submitRating(req)
                    }.onSuccess { res ->
                        status = if (res.success) "OK: ${res.message}" else "FAIL: ${res.message}"
                    }.onFailure {
                        error = UiError(it.message ?: "Unknown error")
                    }
                    isLoading = false
                }
            }) { Text("Pošalji") }
        }

        if (isLoading) CircularProgressIndicator()
        error?.let { Text("Greška: ${it.message}", color = MaterialTheme.colorScheme.error) }
        status?.let { Text(it) }

        info?.let { v ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(v.title, style = MaterialTheme.typography.titleMedium)
                    Text(v.channelName, style = MaterialTheme.typography.bodySmall)
                    Text("Language: ${v.language}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = love, onValueChange = { love = it }, label = { Text("Love 1-3") })
                        OutlinedTextField(value = faith, onValueChange = { faith = it }, label = { Text("Faith 1-3") })
                        OutlinedTextField(value = hope, onValueChange = { hope = it }, label = { Text("Hope 1-3") })
                    }
                }
            }
        }
    }
}
