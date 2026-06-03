package com.youtube.rating.android.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatters {
    private val ddMMyyyyHHmm = ThreadLocal.withInitial {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    }

    private val ratedAtInput = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    private val ddMMyyyy = ThreadLocal.withInitial {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    }

    fun formatDateTime(timestampMillis: Long): String =
        ddMMyyyyHHmm.get()?.format(Date(timestampMillis)).orEmpty()

    fun formatRatedAt(dateString: String): String {
        return try {
            val parsed = ratedAtInput.get()?.parse(dateString)
            ddMMyyyy.get()?.format(parsed ?: Date()).orEmpty()
        } catch (_: Exception) {
            dateString
        }
    }
}

