package com.youtube.rating.android.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.youtube.rating.android.cache.HomeScreenCacheManager
import com.youtube.rating.android.domain.usecase.home.SearchVideosUseCase
import com.youtube.rating.shared.models.PaginatedSearchResponse
import com.youtube.rating.shared.models.VideoSearchResult

@OptIn(ExperimentalPagingApi::class)
class HomeCacheRemoteMediator(
    private val cacheManager: HomeScreenCacheManager,
    private val searchVideosUseCase: SearchVideosUseCase,
    private val params: HomeBrowsePagingParams,
    private val invalidatePagingSource: () -> Unit,
    private val onPageLoaded: (PaginatedSearchResponse) -> Unit
) : RemoteMediator<Int, VideoSearchResult>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, VideoSearchResult>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val lastPage = state.pages.lastOrNull()
                val nextKey = lastPage?.nextKey
                nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val response = searchVideosUseCase(
                searchQuery = params.searchQuery.ifBlank { null },
                minLove = params.minLove,
                maxLove = params.maxLove,
                minFaith = params.minFaith,
                maxFaith = params.maxFaith,
                minHope = params.minHope,
                maxHope = params.maxHope,
                category = params.category,
                sortBy = params.sortBy,
                languages = params.languages,
                page = page,
                perPage = params.perPage,
                forceRefresh = params.forceRefresh || loadType == LoadType.REFRESH
            )

            if (!response.success) {
                return MediatorResult.Error(IllegalStateException("Search failed"))
            }

            val computedHasMore = response.hasMore ||
                (response.totalPages > 0 && page < response.totalPages) ||
                (response.total > page * params.perPage)
            val normalizedResponse = if (computedHasMore == response.hasMore) {
                response
            } else {
                response.copy(hasMore = computedHasMore)
            }

            cacheManager.saveResponse(params.toCacheKey(page), normalizedResponse)
            onPageLoaded(normalizedResponse)
            invalidatePagingSource()

            MediatorResult.Success(endOfPaginationReached = !normalizedResponse.hasMore)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
