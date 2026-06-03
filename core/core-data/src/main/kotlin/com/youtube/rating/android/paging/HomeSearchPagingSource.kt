package com.youtube.rating.android.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.youtube.rating.android.domain.usecase.home.SearchVideosUseCase
import com.youtube.rating.shared.models.VideoSearchResult

class HomeSearchPagingSource(
    private val searchVideosUseCase: SearchVideosUseCase,
    private val params: HomeBrowsePagingParams
) : PagingSource<Int, VideoSearchResult>() {

    override suspend fun load(loadParams: LoadParams<Int>): LoadResult<Int, VideoSearchResult> {
        return try {
            val page = loadParams.key ?: 1
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
                forceRefresh = params.forceRefresh && page == 1
            )

            if (!response.success) {
                return LoadResult.Error(IllegalStateException("Search failed"))
            }

            val hasMore = response.hasMore ||
                (response.totalPages > 0 && page < response.totalPages) ||
                (response.total > page * (if (response.perPage > 0) response.perPage else params.perPage))

            LoadResult.Page(
                data = response.videos,
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (hasMore) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, VideoSearchResult>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null
        return anchorPage.prevKey?.plus(1) ?: anchorPage.nextKey?.minus(1)
    }
}

