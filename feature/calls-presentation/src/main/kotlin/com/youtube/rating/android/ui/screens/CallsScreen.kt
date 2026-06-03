package com.youtube.rating.android.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.designsystem.components.RatingCalloutCard
import com.youtube.rating.core.designsystem.components.RatingEmptyState
import com.youtube.rating.core.designsystem.components.RatingSectionHeader
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import com.youtube.rating.core.designsystem.components.RatingScaffold
import com.youtube.rating.android.viewmodel.CallsViewModel
import com.youtube.rating.core.designsystem.theme.spacing
import io.livekit.android.annotations.Beta
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberConnectionState
import io.livekit.android.compose.state.rememberLocalMedia
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.track.Track
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, Beta::class)
@Composable
fun CallsScreen(viewModel: CallsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val micPermission = Manifest.permission.RECORD_AUDIO
    val cameraPermission = Manifest.permission.CAMERA

    // Permission checks are not observable; force a re-check after permission result callbacks.
    var permissionStateVersion by remember { mutableIntStateOf(0) }
    val hasAllPermissions = remember(context, permissionStateVersion) {
        val hasMic = ContextCompat.checkSelfPermission(context, micPermission) == PackageManager.PERMISSION_GRANTED
        val hasCam = ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED
        hasMic && hasCam
    }

    var pendingJoin by remember { mutableStateOf(false) }
    var permissionRequestedOnce by rememberSaveable { mutableStateOf(false) }
    var showPermissionBlockedDialog by remember { mutableStateOf(false) }

    val defaultLivekitUrl = "wss://rtc.tmbv-hms.com"
    val livekitUrl = (uiState.livekitUrl ?: defaultLivekitUrl).ifBlank { defaultLivekitUrl }
    val livekitToken = uiState.livekitToken.orEmpty()
    val tokenTrimmed = remember(livekitToken) { livekitToken.trim() }
    val tokenLooksLikeJwt = remember(tokenTrimmed) {
        tokenTrimmed.isNotEmpty() && !tokenTrimmed.contains(' ') && tokenTrimmed.count { it == '.' } == 2
    }
    val connect = uiState.connectLiveKit && tokenTrimmed.isNotBlank() && tokenLooksLikeJwt

    var livekitError by remember { mutableStateOf<String?>(null) }

    val prettyDateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }
    val zoneId = remember { ZoneId.systemDefault() }
    val openAppPermissionSettings = remember(context) {
        {
            context.openAppPermissionSettings()
        }
    }

    fun formatIsoOrRaw(value: String?): String =
        com.youtube.rating.android.ui.screens.formatIsoOrRaw(value = value, formatter = prettyDateTimeFormatter)
    fun parseIsoToEpochMillis(value: String?): Long? =
        com.youtube.rating.android.ui.screens.isoToEpochMillis(value = value)
    fun isoFromLocal(dateMillis: Long, hour: Int, minute: Int): String =
        com.youtube.rating.android.ui.screens.isoFromLocal(dateMillis = dateMillis, hour = hour, minute = minute, zoneId = zoneId)

    fun openNativeDateTimePicker() {
        val currentIso = uiState.availabilityFrom
        val initialMillis = parseIsoToEpochMillis(value = currentIso) ?: (System.currentTimeMillis() + 10 * 60 * 1000L)
        val initial = Instant.ofEpochMilli(initialMillis).atZone(zoneId)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, initial.year)
            set(Calendar.MONTH, initial.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, initial.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, initial.hour)
            set(Calendar.MINUTE, initial.minute)
        }

        DatePickerDialog(
            context,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(
                    context,
                    { _, hh, mm ->
                        cal.set(Calendar.HOUR_OF_DAY, hh)
                        cal.set(Calendar.MINUTE, mm)
                        val iso = isoFromLocal(dateMillis = cal.timeInMillis, hour = hh, minute = mm)
                        viewModel.onAvailabilityFromChange(iso)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        permissionStateVersion++
        val granted = (grants[micPermission] == true) && (grants[cameraPermission] == true)
        if (granted && pendingJoin) {
            viewModel.join()
        } else if (pendingJoin && !granted) {
            val canAskMic = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, micPermission) } == true
            val canAskCam = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, cameraPermission) } == true
            val canAskAgain = canAskMic || canAskCam
            if (permissionRequestedOnce && !canAskAgain) showPermissionBlockedDialog = true
        }
        pendingJoin = false
    }

    fun joinCall() {
        if (!hasAllPermissions) {
            pendingJoin = true
            val canAskMic = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, micPermission) } == true
            val canAskCam = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, cameraPermission) } == true
            val canAskAgain = canAskMic || canAskCam
            if (permissionRequestedOnce && !canAskAgain) {
                showPermissionBlockedDialog = true
            } else {
                permissionRequestedOnce = true
                permissionLauncher.launch(arrayOf(micPermission, cameraPermission))
            }
            return
        }
        viewModel.join()
    }

    if (showPermissionBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionBlockedDialog = false },
            title = { Text("Dozvole su blokirane") },
            text = { Text("Android više ne prikazuje dijalog za kameru/mikrofon. Uključi dozvole u postavkama aplikacije.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionBlockedDialog = false
                        openAppPermissionSettings()
                    }
                ) { Text("Otvori postavke") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionBlockedDialog = false }) { Text(Strings.close) }
            }
        )
    }

    @Composable
    fun StatusPill(text: String) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }

    @Composable
    fun LabeledToggle(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        label: String,
        icon: @Composable () -> Unit
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconToggleButton(checked = checked, onCheckedChange = onCheckedChange) { icon() }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    val spacing = MaterialTheme.spacing
    val subtitle = remember(connect, uiState.connectLiveKit) {
        when {
            connect -> "Povezano"
            uiState.connectLiveKit -> "Spajanje..."
            else -> "Spremno"
        }
    }

    RatingScaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            RatingTopAppBar(
                title = Strings.calls,
                subtitle = subtitle
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.lg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                RatingCalloutCard(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    title = "Kako radi",
                    body = "Spajanje je po istom omiljenom psalmu (muško + žensko)."
                )
            }

            item {
                uiState.errorMessage?.let {
                    RatingCalloutCard(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        title = "Greška",
                        body = it,
                        toneSurfaceVariant = false
                    )
                }
                livekitError?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // PROFIL
            item {
                RatingSectionHeader("Profil")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = uiState.displayName,
                                onValueChange = viewModel::onDisplayNameChange,
                                label = { Text("Ime") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                enabled = !connect
                            )
                            OutlinedTextField(
                                value = uiState.ageYears,
                                onValueChange = { raw ->
                                    val next = raw.filter { it.isDigit() }.take(2)
                                    viewModel.onAgeYearsChange(next)
                                },
                                label = { Text("Godine") },
                                singleLine = true,
                                modifier = Modifier.width(110.dp),
                                enabled = !connect,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        val favorite = uiState.favoritePsalm?.trim().orEmpty()
                        val favoriteLabel = favorite.toIntOrNull()?.let { "Psalm $it" } ?: favorite.ifBlank { "-" }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Omiljeni psalm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(favoriteLabel, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            if (uiState.favoritePsalmLocked) {
                                Text("Zaključano", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (!uiState.favoritePsalmLocked) {
                            var favoriteInput by remember(uiState.favoritePsalm) { mutableStateOf(uiState.favoritePsalm.orEmpty()) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = favoriteInput,
                                    onValueChange = { favoriteInput = it.filter { ch -> ch.isDigit() }.take(3) },
                                    label = { Text("1–150") },
                                    supportingText = { Text("Unesi broj psalma.") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    enabled = !connect,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Button(onClick = { viewModel.onFavoritePsalmChange(favoriteInput) }, enabled = !connect) {
                                    Text("Postavi")
                                }
                            }
                        }

                        Text("Spol", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.gender == "male",
                                onClick = { if (!connect) viewModel.onGenderChange("male") },
                                label = { Text("Muško") },
                                enabled = !connect
                            )
                            FilterChip(
                                selected = uiState.gender == "female",
                                onClick = { if (!connect) viewModel.onGenderChange("female") },
                                label = { Text("Žensko") },
                                enabled = !connect
                            )
                        }
                    }
                }
            }

            // DOSTUPNOST
            item {
                RatingSectionHeader("Dostupnost")

                LaunchedEffect(uiState.gender, uiState.favoritePsalm) {
                    viewModel.refreshAvailability()
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Odaberi kada si dostupan (kalendar + vrijeme).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Column(Modifier.weight(1f)) {
                                Text("Kada", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatIsoOrRaw(value = uiState.availabilityFrom), style = MaterialTheme.typography.bodyMedium)
                            }
                            OutlinedButton(onClick = ::openNativeDateTimePicker, enabled = !connect) {
                                Text("Odaberi")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = viewModel::saveAvailability,
                                enabled = !connect && !uiState.isLoading,
                                modifier = Modifier.weight(1f)
                            ) { Text("Spremi") }

                            OutlinedButton(
                                onClick = viewModel::refreshAvailability,
                                enabled = !connect && !uiState.isLoading,
                                modifier = Modifier.weight(1f)
                            ) { Text("Osvježi") }
                        }

                        if (uiState.availabilityItems.isEmpty()) {
                            RatingEmptyState(
                                title = "Nema dostupnih termina",
                                body = "Provjeri kasnije ili promijeni profil (spol/psalm).",
                            )
                        }
                    }
                }
            }

            if (uiState.availabilityItems.isNotEmpty()) {
                item {
                    Text("Tko je dostupan", style = MaterialTheme.typography.titleSmall)
                }
                items(uiState.availabilityItems.take(5)) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatIsoOrRaw(value = item.availableFrom),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // POZIV + VIDEO
            item {
                RatingSectionHeader("Poziv")

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LabeledToggle(
                                checked = uiState.micEnabled,
                                onCheckedChange = viewModel::onMicEnabledChange,
                                label = if (uiState.micEnabled) "Mic" else "Mic off"
                            ) {
                                Icon(
                                    imageVector = if (uiState.micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            LabeledToggle(
                                checked = uiState.camEnabled,
                                onCheckedChange = viewModel::onCamEnabledChange,
                                label = if (uiState.camEnabled) "Cam" else "Cam off"
                            ) {
                                Icon(
                                    imageVector = if (uiState.camEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            OutlinedButton(onClick = viewModel::disconnect, enabled = connect) { Text("Prekini") }
                        }

                        // Primarna akcija
                        Button(
                            onClick = { joinCall() },
                            enabled = !uiState.isLoading && !connect,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text(
                                when {
                                    uiState.isLoading -> "Spajanje…"
                                    !hasAllPermissions -> "Daj dozvole"
                                    else -> "Uđi u poziv"
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        if (!hasAllPermissions) {
                            RatingCalloutCard(
                                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                                title = "Dozvole",
                                body = "Trebaš kameru i mikrofon. Ako si ranije odbio, uključi u postavkama aplikacije.",
                                ctaLabel = "Otvori postavke",
                                onCta = openAppPermissionSettings
                            )
                        }

                        if (!tokenLooksLikeJwt && tokenTrimmed.isNotBlank()) {
                            Text("Token nije JWT (ne mogu se spojiti).", color = MaterialTheme.colorScheme.error)
                        }

                        // LiveKit room
                        RoomScope(
                            url = livekitUrl.trim(),
                            token = tokenTrimmed,
                            audio = uiState.micEnabled,
                            video = uiState.camEnabled,
                            connect = connect,
                            onError = { _, e ->
                                livekitError = buildString {
                                    if (e == null) append("Unknown LiveKit error")
                                    else {
                                        append(e::class.java.simpleName)
                                        val msg = e.message?.trim().orEmpty()
                                        if (msg.isNotBlank()) append(": ").append(msg)
                                    }
                                }
                                Log.e("LiveKitCalls", "error url=${livekitUrl.trim()}", e)
                            }
                        ) {
                            val localMedia = rememberLocalMedia()
                            val connectionState by rememberConnectionState()
                            val cameraTracks by rememberTracks(
                                sources = listOf(Track.Source.CAMERA),
                                usePlaceholders = setOf(Track.Source.CAMERA),
                                onlySubscribed = false
                            )

                            val localTrack = cameraTracks.firstOrNull { it.participant is LocalParticipant }
                            val remoteTracks = cameraTracks.filterNot { it.participant is LocalParticipant }

                            if (connect) {
                                Text(
                                    when (connectionState) {
                                        Room.State.CONNECTING -> "Spajanje…"
                                        Room.State.CONNECTED -> "Spojeno"
                                        else -> "Nije spojeno"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Remote + PiP local
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                ) {
                                    if (remoteTracks.isNotEmpty()) {
                                        VideoTrackView(
                                            trackReference = remoteTracks.first(),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Card(modifier = Modifier.fillMaxSize()) {
                                            RatingEmptyState(
                                                title = "Čekam drugu osobu…",
                                                body = "Kada se spoji, video će se pojaviti ovdje.",
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }

                                    if (localTrack != null && uiState.camEnabled) {
                                        Card(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(10.dp)
                                                .size(120.dp)
                                        ) {
                                            VideoTrackView(
                                                trackReference = localTrack,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { localMedia.switchCamera() },
                                        enabled = connect && uiState.camEnabled,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Promijeni kameru") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
