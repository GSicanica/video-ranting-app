package com.youtube.rating.android.storage

data class FastingEntry(
    val dateKey: String,
    val type: String = FASTING_TYPE_WATER,
    val durationHours: Int = 16,
    val note: String = "",
    val isFasting: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

const val FASTING_TYPE_WATER = "water"
const val FASTING_TYPE_BREAD = "bread"
const val FASTING_TYPE_FULL = "full"
