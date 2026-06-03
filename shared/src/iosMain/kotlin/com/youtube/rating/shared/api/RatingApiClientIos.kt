package com.youtube.rating.shared.api

import com.youtube.rating.shared.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * iOS-specific extensions for RatingApiClient that work around Swift/Kotlin async limitations.
 * These functions use callbacks instead of suspend to avoid "main thread only" restrictions.
 */

private val iosCallbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

/**
 * Fetches all videos with callback-based API for iOS.
 */
fun RatingApiClient.getAllVideosIos(
    onSuccess: (List<VideoStats>) -> Unit,
    onError: (String) -> Unit
) {
    iosCallbackScope.launch {
        try {
            val videos = getAllVideos()
            onSuccess(videos)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

/**
 * Submit a rating for a video.
 */
fun RatingApiClient.submitRatingIos(
    rating: RatingRequest,
    onSuccess: (RatingResponse) -> Unit,
    onError: (String) -> Unit
) {
    iosCallbackScope.launch {
        try {
            val response = submitRating(rating)
            onSuccess(response)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

/**
 * Get video info from YouTube.
 */
fun RatingApiClient.getYouTubeVideoInfoIos(
    videoId: String,
    onSuccess: (YouTubeVideoInfo) -> Unit,
    onError: (String) -> Unit
) {
    iosCallbackScope.launch {
        try {
            val info = getYouTubeVideoInfo(videoId = videoId)
            onSuccess(info)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

/**
 * Search videos by title using V2 API.
 */
fun RatingApiClient.searchVideosIos(
    searchQuery: String? = null,
    page: Int = 1,
    perPage: Int = 20,
    sortBy: String = "latest",
    category: String? = null,
    languages: List<String> = emptyList(),
    onSuccess: (PaginatedSearchResponse) -> Unit,
    onError: (String) -> Unit
) {
    iosCallbackScope.launch {
        try {
            val response = searchVideosV2(searchQuery = searchQuery, minLove = 1, maxLove = 6, minFaith = 1, maxFaith = 6, minHope = 1, maxHope = 6, category = category, sortBy = sortBy, languages = languages, page = page, perPage = perPage, forceRefresh = false)
            onSuccess(response)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

/**
 * Advanced V2 search with rating ranges (for Home tab parity with Android).
 */
fun RatingApiClient.searchVideosV2Ios(
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
    forceRefresh: Boolean = false,
    onSuccess: (PaginatedSearchResponse) -> Unit,
    onError: (String) -> Unit
) {
    iosCallbackScope.launch {
        try {
            val response = searchVideosV2(
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
            onSuccess(response)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

/**
 * Fetch top videos for featured carousel.
 */
fun RatingApiClient.getTopVideosIos(
    type: String = "popular",
    category: String? = null,
    language: String? = null,
    limit: Int = 10,
    onSuccess: (TopVideosResponse) -> Unit,
    onError: (String) -> Unit
) {
    iosCallbackScope.launch {
        try {
            val response = getTopVideos(type = type, category = category, language = language, limit = limit)
            onSuccess(response)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

fun RatingApiClient.getPersonalizedFeedIos(
    userToken: String,
    limit: Int = 50,
    category: String? = null,
    languages: List<String> = emptyList(),
    forceRefresh: Boolean = false,
    onSuccess: (PersonalizedFeedResponse) -> Unit,
    onError: (String) -> Unit
) {
    iosCallbackScope.launch {
        try {
            val response = getPersonalizedFeed(
                userToken = userToken,
                limit = limit,
                category = category,
                languages = languages,
                forceRefresh = forceRefresh
            )
            onSuccess(response)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

/**
 * Fetch popular search terms for suggestions.
 */
fun RatingApiClient.getPopularSearchTermsIos(
    limit: Int = 6,
    language: String = "hr",
    onSuccess: (List<String>) -> Unit,
    onError: (String) -> Unit
) {
    iosCallbackScope.launch {
        try {
            val response = getPopularSearchTerms(limit = limit, language = language)
            onSuccess(response)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

/**
 * Sync favorites with server.
 */
fun RatingApiClient.syncFavoritesIos(
    deviceId: String,
    favorites: List<FavoriteVideoDto>,
    onSuccess: (SyncFavoritesResponse) -> Unit,
    onError: (String) -> Unit
) {
    iosCallbackScope.launch {
        try {
            val response = syncFavorites(userToken = deviceId, favorites = favorites)
            onSuccess(response)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

fun RatingApiClient.getRatedVideosIos(
    deviceId: String,
    page: Int,
    perPage: Int,
    category: String?,
    sortBy: String,
    onSuccess: (RatedVideosResponse) -> Unit,
    onError: (String) -> Unit
) {
    iosCallbackScope.launch {
        try {
            val response = getRatedVideos(userToken = deviceId, page = page, perPage = perPage, category = category, sortBy = sortBy)
            onSuccess(response)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}
