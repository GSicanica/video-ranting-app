package com.youtube.rating.android.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

internal fun Context.openAppPermissionSettings() {
    runCatching {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }.onFailure { e -> Log.e("LiveKitCalls", "Failed to open app settings", e) }
}

internal fun formatIsoOrRaw(value: String?, formatter: DateTimeFormatter): String {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return "-"
    return try {
        val dt = OffsetDateTime.parse(raw)
        dt.format(formatter)
    } catch (_: Exception) {
        raw
    }
}

internal fun isoToEpochMillis(value: String?): Long? {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return null
    return try {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

internal fun isoFromLocal(dateMillis: Long, hour: Int, minute: Int, zoneId: ZoneId): String {
    val localDate = Instant.ofEpochMilli(dateMillis).atZone(zoneId).toLocalDate()
    val localTime = java.time.LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    val odt = localDate.atTime(localTime).atZone(zoneId).toOffsetDateTime()
    return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

