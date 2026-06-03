package com.youtube.rating.android.data.repository

import com.youtube.rating.android.source.HomeContentSourceRegistry
import com.youtube.rating.shared.models.ApiResponse
import com.youtube.rating.shared.models.HomeCategoriesResponse
import com.youtube.rating.shared.models.PaginatedSearchResponse
import com.youtube.rating.shared.models.TopVideosResponse

class HomeContentRepositoryImpl(
    private val sourceRegistry: HomeContentSourceRegistry
) : HomeContentRepository {
    private val source get() = sourceRegistry.active()

    override suspend fun getPopularSearchTerms(limit: Int, language: String): List<String> {
        return source.getPopularSearchTerms(limit, language)
    }

    override suspend fun getHomeCategories(forceRefresh: Boolean): HomeCategoriesResponse {
        return source.getHomeCategories(forceRefresh)
    }

    override suspend fun getTopVideos(
        type: String,
        category: String?,
        language: String?,
        limit: Int,
        range: String?
    ): TopVideosResponse {
        return source.getTopVideos(type, category, language, limit, range)
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
        return source.searchVideosV2(
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
        return source.reportVideo(videoId, userToken, reason)
    }

    override suspend fun trackSearchTerm(term: String, language: String): ApiResponse {
        return source.trackSearchTerm(term, language)
    }
}
