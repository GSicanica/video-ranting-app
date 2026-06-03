package com.youtube.rating.android.data.repository

import com.youtube.rating.android.domain.repository.HomeContentGateway
import com.youtube.rating.shared.models.ApiResponse
import com.youtube.rating.shared.models.HomeCategoriesResponse
import com.youtube.rating.shared.models.PaginatedSearchResponse
import com.youtube.rating.shared.models.TopVideosResponse

interface HomeContentRepository : HomeContentGateway {
    override suspend fun getPopularSearchTerms(limit: Int, language: String): List<String>
    override suspend fun getHomeCategories(forceRefresh: Boolean): HomeCategoriesResponse
    override suspend fun getTopVideos(
        type: String,
        category: String?,
        language: String?,
        limit: Int,
        range: String?
    ): TopVideosResponse

    override suspend fun searchVideosV2(
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

    override suspend fun reportVideo(videoId: String, userToken: String, reason: String): ApiResponse
    override suspend fun trackSearchTerm(term: String, language: String): ApiResponse
}
