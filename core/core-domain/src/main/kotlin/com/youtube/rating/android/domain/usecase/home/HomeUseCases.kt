package com.youtube.rating.android.domain.usecase.home

import com.youtube.rating.android.domain.repository.HomeContentGateway
import com.youtube.rating.shared.models.ApiResponse
import com.youtube.rating.shared.models.HomeCategoriesResponse
import com.youtube.rating.shared.models.PaginatedSearchResponse
import com.youtube.rating.shared.models.TopVideosResponse

class GetPopularSearchTermsUseCase(
    private val repo: HomeContentGateway
) {
    suspend operator fun invoke(limit: Int, language: String): List<String> =
        repo.getPopularSearchTerms(limit, language)
}

class GetHomeCategoriesUseCase(
    private val repo: HomeContentGateway
) {
    suspend operator fun invoke(forceRefresh: Boolean): HomeCategoriesResponse =
        repo.getHomeCategories(forceRefresh)
}

class GetTopVideosUseCase(
    private val repo: HomeContentGateway
) {
    suspend operator fun invoke(
        type: String,
        category: String?,
        language: String?,
        limit: Int,
        range: String?
    ): TopVideosResponse = repo.getTopVideos(type, category, language, limit, range)
}

class SearchVideosUseCase(
    private val repo: HomeContentGateway
) {
    suspend operator fun invoke(
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
    ): PaginatedSearchResponse = repo.searchVideosV2(
        searchQuery,
        minLove,
        maxLove,
        minFaith,
        maxFaith,
        minHope,
        maxHope,
        category,
        sortBy,
        languages,
        page,
        perPage,
        forceRefresh
    )
}

class ReportVideoUseCase(
    private val repo: HomeContentGateway
) {
    suspend operator fun invoke(videoId: String, userToken: String, reason: String): ApiResponse =
        repo.reportVideo(videoId, userToken, reason)
}
