package com.youtube.rating.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class PersonalizedFeedResponse(
    val success: Boolean = false,
    val coldStart: Boolean = false,
    val count: Int = 0,
    val videos: List<VideoSearchResult> = emptyList(),
    val message: String? = null
)

