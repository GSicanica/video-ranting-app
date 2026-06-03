package com.youtube.rating.ioscomposeapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalUriHandler
import com.youtube.rating.ioscomposeapp.platform.PlatformUserToken
import com.youtube.rating.shared.BASE_URL
import com.youtube.rating.shared.RuntimeConfig
import com.youtube.rating.shared.api.RatingApiClient
import kotlinx.coroutines.launch

@Composable
internal fun SettingsScreen(
    api: RatingApiClient
) {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var baseUrlInput by remember { mutableStateOf(BASE_URL) }
    var cachedToken by remember { mutableStateOf(PlatformUserToken.getCachedOrNull()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        uriHandler.openUri("https://donate.stripe.com/3cI7sK8vcdBC6871e17ss00")
                    }) { Text("Donate") }
                    Button(onClick = {
                        uriHandler.openUri("https://tmbv-hms.com/privacy-policy.php")
                    }) { Text("Privacy") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Base URL", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = { baseUrlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("BASE_URL") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val normalized = baseUrlInput.trim()
                        RuntimeConfig.setBaseUrls(baseUrl = normalized, dataBaseUrl = normalized)
                        status = "Saved base URL"
                    }) { Text("Spremi") }
                    Button(onClick = {
                        scope.launch {
                            status = "Checking…"
                            status = runCatching {
                                val resp = api.healthCheck()
                                if (resp.success) "OK: ${resp.message}" else "FAIL: ${resp.message}"
                            }.getOrElse { "Error: ${it.message}" }
                        }
                    }) { Text("Health") }
                }
                status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("User token", style = MaterialTheme.typography.titleMedium)
                Text(cachedToken ?: "-", style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    scope.launch {
                        cachedToken = PlatformUserToken.getOrCreate(api)
                        status = "Token ready"
                    }
                }) { Text("Create/Load token") }
            }
        }
    }
}
