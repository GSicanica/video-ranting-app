package com.youtube.rating.android.feature.running.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.youtube.rating.android.feature.running.domain.RoutePoint
import com.youtube.rating.android.feature.running.domain.RunningEntry
import com.youtube.rating.android.feature.running.domain.RunningStats
import com.youtube.rating.android.feature.running.viewmodel.RunningViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.DecimalFormat
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningScreen(
    onBack: () -> Unit,
    viewModel: RunningViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isGpsTracking by viewModel.isGpsTracking.collectAsStateWithLifecycle()
    val activeDistanceKm by viewModel.activeDistanceKm.collectAsStateWithLifecycle()
    val activeDurationSeconds by viewModel.activeDurationSeconds.collectAsStateWithLifecycle()
    val activePaceMinPerKm by viewModel.activePaceMinPerKm.collectAsStateWithLifecycle()
    val activeRoutePoints by viewModel.activeRoutePoints.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var distanceInput by remember { mutableStateOf("") }
    var durationInput by remember { mutableStateOf("") }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showDiscardGpsDialog by remember { mutableStateOf(false) }
    var selectedRoutePreview by remember { mutableStateOf<List<RoutePoint>>(emptyList()) }
    var selectedRoutePreviewEntryId by remember { mutableStateOf<Long?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val hasLocation = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocation) {
            viewModel.startGpsTracking()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val startTracking = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.startGpsTracking()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Trcanje") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Nazad",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RunningStatsCard(stats = stats)
            }

            item {
                GpsTrackingCard(
                    isTracking = isGpsTracking,
                    activeDistanceKm = activeDistanceKm,
                    activeDurationSeconds = activeDurationSeconds,
                    activePaceMinPerKm = activePaceMinPerKm,
                    activePoints = activeRoutePoints.size,
                    onStartTracking = startTracking,
                    onStopAndSave = { viewModel.stopGpsTracking(saveSession = true) },
                    onStopAndDiscard = { showDiscardGpsDialog = true },
                )
            }

            if (activeRoutePoints.size >= 2) {
                item {
                    RouteMapCard(
                        title = "Aktivna ruta",
                        points = activeRoutePoints,
                    )
                }
            }

            item {
                RunningEntryForm(
                    distanceInput = distanceInput,
                    durationInput = durationInput,
                    onDistanceChanged = { distanceInput = it },
                    onDurationChanged = { durationInput = it },
                    onSave = {
                        val distance = distanceInput.toDoubleOrNull()
                        val duration = durationInput.toIntOrNull()
                        if (distance != null && duration != null) {
                            viewModel.addEntry(distanceKm = distance, durationMinutes = duration)
                            distanceInput = ""
                            durationInput = ""
                        }
                    },
                    enabled = !isGpsTracking,
                )
            }

            if (entries.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = { showClearAllDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Obriši sve treninge",
                        )
                        Text(text = "Obriši sve", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Text(
                        text = "Nema unosa. Dodaj prvi trening trcanja.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(
                    items = entries,
                    key = { it.id },
                ) { entry ->
                    RunningEntryCard(
                        entry = entry,
                        onDelete = {
                            if (selectedRoutePreviewEntryId == entry.id) {
                                selectedRoutePreviewEntryId = null
                                selectedRoutePreview = emptyList()
                            }
                            viewModel.deleteEntry(entry.id)
                        },
                        onShowRoute = {
                            selectedRoutePreviewEntryId = entry.id
                            selectedRoutePreview = entry.routePoints
                        },
                    )
                }
            }

            if (selectedRoutePreview.size >= 2) {
                item {
                    RouteMapCard(
                        title = "Spremljena ruta",
                        points = selectedRoutePreview,
                    )
                }
            }
        }

        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                title = { Text("Obriši sve treninge?") },
                text = { Text("Ova akcija trajno briše sve spremljene treninge trčanja.") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearAllDialog = false
                        viewModel.clearAllEntries()
                        selectedRoutePreviewEntryId = null
                        selectedRoutePreview = emptyList()
                    }) {
                        Text("Obriši")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) { Text("Odustani") }
                },
            )
        }

        if (showDiscardGpsDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardGpsDialog = false },
                title = { Text("Zaustavi bez spremanja?") },
                text = { Text("Aktivna GPS sesija neće biti spremljena.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDiscardGpsDialog = false
                        viewModel.stopGpsTracking(saveSession = false)
                    }) {
                        Text("Zaustavi")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardGpsDialog = false }) { Text("Natrag") }
                },
            )
        }

    }
}

@Composable
private fun RunningStatsCard(stats: RunningStats) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Sažetak trčanja",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Treninga: ${stats.totalRuns}")
                Text("Km: ${formatKilometers(stats.totalDistanceKm)}")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Min: ${stats.totalDurationMinutes}")
                Text("Avg tempo: ${formatPace(stats.averagePaceMinPerKm)}")
            }
        }
    }
}

@Composable
private fun RunningEntryForm(
    distanceInput: String,
    durationInput: String,
    onDistanceChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onSave: () -> Unit,
    enabled: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Novi running trening",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = distanceInput,
                onValueChange = onDistanceChanged,
                label = { Text("Distanca (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled,
            )

            OutlinedTextField(
                value = durationInput,
                onValueChange = onDurationChanged,
                label = { Text("Trajanje (min)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled,
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            ) {
                Text("Spremi trening")
            }
            if (!enabled) {
                Text(
                    text = "Ručni unos je zaključan tijekom aktivnog GPS praćenja.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RunningEntryCard(
    entry: RunningEntry,
    onDelete: () -> Unit,
    onShowRoute: () -> Unit,
) {
    val formattedTime = remember(entry.createdAtMillis) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(entry.createdAtMillis))
    }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${formatKilometers(entry.distanceKm)} km za ${entry.durationMinutes} min",
                    style = MaterialTheme.typography.titleSmall,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Obriši trening",
                    )
                }
            }
            Text(
                text = "Tempo: ${formatPace(entry.paceMinPerKm)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (entry.routePoints.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "GPS točke: ${entry.routePoints.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = onShowRoute) {
                        Text("Prikaži rutu")
                    }
                }
            }
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GpsTrackingCard(
    isTracking: Boolean,
    activeDistanceKm: Double,
    activeDurationSeconds: Long,
    activePaceMinPerKm: Double,
    activePoints: Int,
    onStartTracking: () -> Unit,
    onStopAndSave: () -> Unit,
    onStopAndDiscard: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "GPS praćenje",
                )
                Text(
                    text = "GPS praćenje rute",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Text("Distanca: ${formatKilometers(activeDistanceKm)} km")
            Text("Trajanje: ${formatDuration(activeDurationSeconds)}")
            Text("Tempo: ${formatActivePace(activePaceMinPerKm)}")
            Text("Točke rute: $activePoints")

            if (!isTracking) {
                Button(
                    onClick = onStartTracking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Pokreni GPS")
                    Text("Pokreni GPS", modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Button(
                    onClick = onStopAndSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Zaustavi i spremi")
                    Text("Zaustavi i spremi", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = onStopAndDiscard,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Zaustavi bez spremanja")
                }
            }
        }
    }
}

@Composable
private fun RouteMapCard(
    title: String,
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return

    val cameraPositionState = rememberCameraPositionState()
    val latLngPoints = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }

    LaunchedEffect(latLngPoints) {
        val boundsBuilder = LatLngBounds.Builder()
        latLngPoints.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()
        cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 96))
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 4.dp),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true,
                ),
            ) {
                Polyline(points = latLngPoints)
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatKilometers(value: Double): String = decimalFormatter.format(value)

private fun formatPace(value: Double): String = "${decimalFormatter.format(value)} min/km"

private fun formatActivePace(value: Double): String = if (value > 0.0) formatPace(value) else "-"

private val decimalFormatter = DecimalFormat("0.00")
