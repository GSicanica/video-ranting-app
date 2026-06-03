package com.youtube.rating.android.source

import com.youtube.rating.shared.models.ApiResponse
import com.youtube.rating.shared.models.HomeCategoriesResponse
import com.youtube.rating.shared.models.PaginatedSearchResponse
import com.youtube.rating.shared.models.TopVideosResponse

/**
 * Pluggable home content source (plugin-style abstraction).
 * Add new implementations to provide alternate content feeds.
 */
interface HomeContentSource {
    val id: String
    val displayName: String

    suspend fun getPopularSearchTerms(limit: Int = 9, language: String = "hr"): List<String>

    suspend fun getHomeCategories(forceRefresh: Boolean = false): HomeCategoriesResponse

    suspend fun getTopVideos(
        type: String = "rated",
        category: String? = null,
        language: String? = null,
        limit: Int = 10,
        range: String? = null
    ): TopVideosResponse

    suspend fun searchVideosV2(
        searchQuery: String? = null,
        minLove: Int = 1,
        maxLove: Int = 3,
        minFaith: Int = 1,
        maxFaith: Int = 3,
        minHope: Int = 1,
        maxHope: Int = 3,
        category: String? = null,
        sortBy: String = "latest",
        languages: List<String> = emptyList(),
        page: Int = 1,
        perPage: Int = 20,
        forceRefresh: Boolean = false
    ): PaginatedSearchResponse

    suspend fun reportVideo(
        videoId: String,
        userToken: String,
        reason: String = "inappropriate"
    ): ApiResponse

    suspend fun trackSearchTerm(term: String, language: String = "unknown"): ApiResponse
}
