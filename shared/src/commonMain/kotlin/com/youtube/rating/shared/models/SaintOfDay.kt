package com.youtube.rating.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class SaintOfDayNav(
    val date: String? = null, // yyyy-MM-dd or MM-dd
    val title: String? = null,
    val url: String? = null
)

@Serializable
data class SaintOfDayResponse(
    val success: Boolean,
    val title: String? = null,
    val image: String? = null,
    val content: String? = null,
    val fetchedAt: String? = null,
    val source: String? = null,
    val url: String? = null,
    val date: String? = null,
    val prev: SaintOfDayNav? = null,
    val next: SaintOfDayNav? = null,
    val message: String? = null
)
