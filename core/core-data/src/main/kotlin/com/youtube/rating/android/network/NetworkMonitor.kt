package com.youtube.rating.android.network

import android.content.Context
import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Network state monitor
 * Provides real-time network connectivity status
 * 
 * ✅ FIXED: Uses applicationContext to prevent memory leaks
 * 
 * Usage:
 * ```
 * val networkMonitor = NetworkMonitor(context = context)
 * networkMonitor.isOnline.collect { isOnline ->
 *     if (isOnline) {
 *         // Make API calls
 *     } else {
 *         // Show offline message
 *     }
 * }
 * ```
 */
class NetworkMonitor(context: Context) {
    // ✅ FIX: Use applicationContext to prevent Activity memory leak
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * Flow that emits network connectivity status
     * true = online, false = offline
     */
    @SuppressLint("MissingPermission")
    val isOnline: Flow<Boolean> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        @SuppressLint("MissingPermission")
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val networks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                val caps = cm.getNetworkCapabilities(network)
                val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

                if (hasInternet && isValidated) {
                    networks.add(network)
                    trySend(true)
                }
            }

            override fun onLost(network: Network) {
                networks.remove(network)
                trySend(networks.isNotEmpty())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val isValidated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                
                if (hasInternet && isValidated) {
                    networks.add(network)
                    trySend(true)
                } else {
                    networks.remove(network)
                    trySend(networks.isNotEmpty())
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, callback)

        // Send initial state
        trySend(isCurrentlyOnline())

        awaitClose {
            cm.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Check current network state synchronously
     */
    fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Get current network type
     */
    fun getNetworkType(): NetworkType {
        if (!isCurrentlyOnline()) return NetworkType.NONE

        val network = connectivityManager?.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }

    /**
     * Check if connection is metered (cellular data with possible charges)
     */
    fun isMeteredConnection(): Boolean {
        return connectivityManager?.isActiveNetworkMetered ?: false
    }
}

/**
 * Network type enum
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER,
    NONE
}

/**
 * Extension function to check if network calls should be made
 * considering network state and user preferences
 */
fun NetworkMonitor.shouldMakeNetworkCall(
    requireUnmeteredOnly: Boolean = false
): Boolean {
    if (!isCurrentlyOnline()) return false
    
    return if (requireUnmeteredOnly) {
        !isMeteredConnection()
    } else {
        true
    }
}
