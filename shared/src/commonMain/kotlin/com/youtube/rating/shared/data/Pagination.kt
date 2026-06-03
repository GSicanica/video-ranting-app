package com.youtube.rating.shared.data

/**
 * Pagination state for infinite scroll
 */
data class PaginationState<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val totalCount: Int? = null
) {
    val isEmpty: Boolean
        get() = items.isEmpty() && !isLoading
    
    val canLoadMore: Boolean
        get() = hasMore && !isLoading && !isLoadingMore && error == null
}

/**
 * Pagination actions
 */
sealed class PaginationAction {
    object Refresh : PaginationAction()
    object LoadMore : PaginationAction()
    object Retry : PaginationAction()
}

/**
 * Paginated response from API
 */
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val hasMore: Boolean
) {
    val totalPages: Int
        get() = (totalCount + pageSize - 1) / pageSize
}

/**
 * Helper to manage pagination logic
 */
class PaginationHelper<T>(
    private val pageSize: Int = 20,
    private val loadPage: suspend (page: Int, pageSize: Int) -> PaginatedResponse<T>
) {
    private var currentState = PaginationState<T>(pageSize = pageSize)
    
    suspend fun refresh(): PaginationState<T> {
        currentState = currentState.copy(
            isLoading = true,
            error = null,
            page = 1
        )
        
        return try {
            val response = loadPage(1, pageSize)
            currentState = PaginationState(
                items = response.data,
                page = 1,
                pageSize = pageSize,
                hasMore = response.hasMore,
                totalCount = response.totalCount,
                isLoading = false
            )
            currentState
        } catch (e: Exception) {
            currentState = currentState.copy(
                isLoading = false,
                error = e.message ?: "Unknown error"
            )
            currentState
        }
    }
    
    suspend fun loadMore(): PaginationState<T> {
        if (!currentState.canLoadMore) return currentState
        
        currentState = currentState.copy(isLoadingMore = true, error = null)
        
        return try {
            val nextPage = currentState.page + 1
            val response = loadPage(nextPage, pageSize)
            
            currentState = currentState.copy(
                items = currentState.items + response.data,
                page = nextPage,
                hasMore = response.hasMore,
                totalCount = response.totalCount,
                isLoadingMore = false
            )
            currentState
        } catch (e: Exception) {
            currentState = currentState.copy(
                isLoadingMore = false,
                error = e.message ?: "Unknown error"
            )
            currentState
        }
    }
    
    fun getCurrentState(): PaginationState<T> = currentState
}
