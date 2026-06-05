package com.youtube.rating.android.source

import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.ApiResponse
import com.youtube.rating.shared.models.HomeCategoriesResponse
import com.youtube.rating.shared.models.PaginatedSearchResponse
import com.youtube.rating.shared.models.TopVideosResponse

/**
 * Default home content source backed by RatingApiClient.
 */
class RatingApiHomeContentSource(
    private val apiClient: RatingApiClient
) : HomeContentSource {
    override val id: String = "rating_api"
    override val displayName: String = "Rating API"

    override suspend fun getPopularSearchTerms(limit: Int, language: String): List<String> {
        return apiClient.getPopularSearchTerms(limit, language)
    }

    override suspend fun getHomeCategories(forceRefresh: Boolean): HomeCategoriesResponse {
        return apiClient.getHomeCategories(forceRefresh)
    }

    override suspend fun getTopVideos(
        type: String,
        category: String?,
        language: String?,
        limit: Int,
        range: String?
    ): TopVideosResponse {
        return apiClient.getTopVideos(type, category, language, limit, range)
    }

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
    ): PaginatedSearchResponse {
        return apiClient.searchVideosV2(
            searchQuery = searchQuery,
            minLove = minLove,
            maxLove = maxLove,
            minFaith = minFaith,
            maxFaith = maxFaith,
            minHope = minHope,
            maxHope = maxHope,
            category = category,
            sortBy = sortBy,
            languages = languages,
            page = page,
            perPage = perPage,
            forceRefresh = forceRefresh
        )
    }

    override suspend fun reportVideo(videoId: String, userToken: String, reason: String): ApiResponse {
        return apiClient.reportVideo(videoId, userToken, reason)
    }
}
