package com.youtube.rating.android.paging

import com.youtube.rating.android.cache.SearchCacheKey

/**
 * Parameters that define a browse query for paging + cache.
 * refreshKey is used to force pager recreation even if filters are unchanged.
 */
data class HomeBrowsePagingParams(
    val searchQuery: String,
    val category: String?,
    val minLove: Int,
    val maxLove: Int,
    val minFaith: Int,
    val maxFaith: Int,
    val minHope: Int,
    val maxHope: Int,
    val languages: List<String>,
    val sortBy: String,
    val perPage: Int,
    val refreshKey: Long,
    val forceRefresh: Boolean
) {
    fun toCacheKey(page: Int): SearchCacheKey = SearchCacheKey(
        searchQuery = searchQuery,
        category = category,
        minLove = minLove,
        maxLove = maxLove,
        minFaith = minFaith,
        maxFaith = maxFaith,
        minHope = minHope,
        maxHope = maxHope,
        languages = languages,
        sortBy = sortBy,
        page = page,
        perPage = perPage
    )
}
