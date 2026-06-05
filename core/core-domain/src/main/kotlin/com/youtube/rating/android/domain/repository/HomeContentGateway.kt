package com.youtube.rating.android.domain.repository

import com.youtube.rating.shared.models.ApiResponse
import com.youtube.rating.shared.models.HomeCategoriesResponse
import com.youtube.rating.shared.models.PaginatedSearchResponse
import com.youtube.rating.shared.models.TopVideosResponse

interface HomeContentGateway {
    suspend fun getPopularSearchTerms(limit: Int, language: String): List<String>
    suspend fun getHomeCategories(forceRefresh: Boolean): HomeCategoriesResponse
    suspend fun getTopVideos(
        type: String,
        category: String?,
        language: String?,
        limit: Int,
        range: String?
    ): TopVideosResponse

    suspend fun searchVideosV2(
        searchQuery: String?,
        minLove: Int,
        maxLove: Int,
        minFaith: Int,
        maxFaith: Int,
        minHope: Int,
        maxHope: Int,
        category: String?,
        sortBy: String,
        languages: List<String>,
        page: Int,
        perPage: Int,
        forceRefresh: Boolean
    ): PaginatedSearchResponse

    suspend fun reportVideo(videoId: String, userToken: String, reason: String): ApiResponse
}
