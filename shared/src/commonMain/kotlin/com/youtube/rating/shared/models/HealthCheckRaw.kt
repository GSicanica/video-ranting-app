package com.youtube.rating.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Current backend `/api/health.php` format (not wrapped in `success/message`).
 *
 * Kotlin clients map this into [HealthCheckResponse] for backward compatibility.
 */
@Serializable
data class HealthCheckRawResponse(
    val status: String,
    val timestamp: String,
    val checks: Map<String, HealthCheckRawItem>,
    val version: String? = null,
    val uptime: String? = null
)

@Serializable
data class HealthCheckRawItem(
    val status: String,
    val message: String,
    @SerialName("duration_ms")
    val durationMs: Double? = null
)

