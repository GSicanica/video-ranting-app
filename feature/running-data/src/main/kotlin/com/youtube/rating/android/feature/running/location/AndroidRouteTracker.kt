package com.youtube.rating.android.feature.running.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.youtube.rating.android.feature.running.domain.RoutePoint
import com.youtube.rating.android.feature.running.domain.RouteTracker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidRouteTracker(
    private val context: Context,
) : RouteTracker {

    private companion object {
        const val MIN_TIME_MS = 2000L
        const val MIN_DISTANCE_METERS = 2f
        const val MAX_ACCEPTED_ACCURACY_METERS = 30f
        const val MAX_ACCEPTED_SPEED_METERS_PER_SECOND = 8.5
    }

    @SuppressLint("MissingPermission")
    override fun routeUpdates(): Flow<RoutePoint> = callbackFlow {
        if (!hasLocationPermission()) {
            close(IllegalStateException("Location permission not granted"))
            return@callbackFlow
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> {
                close(IllegalStateException("Location provider disabled"))
                return@callbackFlow
            }
        }

        var lastAcceptedPoint: RoutePoint? = null

        val listener = LocationListener { location: Location ->
            if (location.hasAccuracy() && location.accuracy > MAX_ACCEPTED_ACCURACY_METERS) return@LocationListener
            if (location.hasSpeed() && location.speed > MAX_ACCEPTED_SPEED_METERS_PER_SECOND) return@LocationListener

            val candidate = RoutePoint(
                latitude = location.latitude,
                longitude = location.longitude,
                timestampMillis = location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
            )
            val previous = lastAcceptedPoint
            if (previous != null && previous.latitude == candidate.latitude && previous.longitude == candidate.longitude) {
                return@LocationListener
            }

            lastAcceptedPoint = candidate
            trySend(candidate)
        }

        runCatching {
            locationManager.requestLocationUpdates(
                provider,
                MIN_TIME_MS,
                MIN_DISTANCE_METERS,
                listener,
                Looper.getMainLooper(),
            )
        }.onFailure {
            close(it)
            return@callbackFlow
        }

        runCatching { locationManager.getLastKnownLocation(provider) }
            .getOrNull()
            ?.takeIf { !it.hasAccuracy() || it.accuracy <= MAX_ACCEPTED_ACCURACY_METERS }
            ?.let { lastLocation ->
                val seed = RoutePoint(
                    latitude = lastLocation.latitude,
                    longitude = lastLocation.longitude,
                    timestampMillis = lastLocation.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                )
                lastAcceptedPoint = seed
                trySend(seed)
            }

        awaitClose {
            runCatching { locationManager.removeUpdates(listener) }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
