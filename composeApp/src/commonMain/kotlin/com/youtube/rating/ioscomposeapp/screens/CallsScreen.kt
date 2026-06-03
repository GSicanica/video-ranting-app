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
import com.youtube.rating.ioscomposeapp.platform.PlatformUserToken
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.PsalmAvailabilityItem
import kotlinx.coroutines.launch

@Composable
internal fun CallsScreen(
    api: RatingApiClient
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiError?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var displayName by remember { mutableStateOf("") }
    var favoritePsalm by remember { mutableStateOf("23") }
    var gender by remember { mutableStateOf("male") }
    var matches by remember { mutableStateOf<List<PsalmAvailabilityItem>>(emptyList()) }

    suspend fun refreshMatches() {
        isLoading = true
        error = null
        runCatching {
            val token = PlatformUserToken.getOrCreate(api)
            val resp = api.listPsalmCallAvailability(
                userToken = token,
                gender = gender,
                favoritePsalm = favoritePsalm.trim()
            )
            if (!resp.success) {
                error = UiError(resp.message ?: "Server error")
            }
            matches = resp.items
        }.onFailure {
            error = UiError(it.message ?: "Unknown error")
        }
        isLoading = false
    }

    suspend fun findRoom() {
        isLoading = true
        error = null
        status = null
        runCatching {
            val token = PlatformUserToken.getOrCreate(api)
            val roomResp = api.getPsalmCallRoom(
                userToken = token,
                displayName = displayName.ifBlank { "iOS User" },
                gender = gender,
                notMarried = false,
                selectedTokens = listOf(favoritePsalm.trim()),
                favoritePsalm = favoritePsalm.trim()
            )
            if (!roomResp.success || roomResp.room.isNullOrBlank()) {
                status = "Room: ${roomResp.message ?: "not found"}"
                return@runCatching
            }
            val roomId = roomResp.room ?: return@runCatching
            val tokenResp = api.getLiveKitToken(
                userToken = token,
                room = roomId,
                identity = token.take(24),
                name = displayName.ifBlank { "iOS User" }
            )
            status = if (tokenResp.success) {
                "Room $roomId ready"
            } else {
                "Token error: ${tokenResp.message ?: "unknown"}"
            }
        }.onFailure {
            error = UiError(it.message ?: "Unknown error")
        }
        isLoading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Calls", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") }
                )
                OutlinedTextField(
                    value = favoritePsalm,
                    onValueChange = { favoritePsalm = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Favorite psalm") }
                )
                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Gender (male/female)") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !isLoading,
                        onClick = { scope.launch { refreshMatches() } }
                    ) { Text("Refresh") }
                    Button(
                        enabled = !isLoading,
                        onClick = { scope.launch { findRoom() } }
                    ) { Text("Join") }
                }
            }
        }

        if (isLoading) CircularProgressIndicator()
        error?.let { Text("Error: ${it.message}", color = MaterialTheme.colorScheme.error) }
        status?.let { Text(it) }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(matches, key = { "${it.displayName}-${it.availableFrom}-${it.availableTo}" }) { item ->
                Card {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(item.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "From: ${item.availableFrom ?: "-"}  To: ${item.availableTo ?: "-"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
