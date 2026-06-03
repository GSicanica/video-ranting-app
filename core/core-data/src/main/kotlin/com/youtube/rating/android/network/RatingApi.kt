package com.youtube.rating.android.network

import com.youtube.rating.shared.data.AddEncouragementBody
import com.youtube.rating.shared.data.CreatePrayerRequestBody
import com.youtube.rating.shared.data.EncouragementDto
import com.youtube.rating.shared.data.PrayerPageDto
import com.youtube.rating.shared.data.PrayerRequestDto
import com.youtube.rating.shared.data.SetPrayerPrayedBody
import com.youtube.rating.shared.models.*

/**
 * Abstraction over the network API client used by the app.
 * Provides a single interface to the backend API so consumers can depend on an interface
 * instead of the concrete `RatingApiClient` implementation.
 */
interface RatingApi {
    suspend fun submitRating(rating: RatingRequest): RatingResponse
    suspend fun getVideoStats(videoId: String): VideoStats
    suspend fun getAllVideos(): List<VideoStats>
    suspend fun getVideosList(page: Int = 1, limit: Int = 20, category: String? = null, language: String? = null): VideosListResponse
    suspend fun searchVideosNew(query: String, page: Int = 1, limit: Int = 20, category: String? = null, language: String? = null): SearchVideosResponse
    suspend fun getYouTubeVideoInfo(videoId: String): YouTubeVideoInfo
    suspend fun searchVideos(searchQuery: String? = null, minLove: Int = 0, minFaith: Int = 0, minHope: Int = 0, category: String? = null, sortBy: String = "latest", languages: List<String> = emptyList()): List<VideoSearchResult>
    suspend fun searchVideosV2(searchQuery: String? = null, minLove: Int = 1, maxLove: Int = 3, minFaith: Int = 1, maxFaith: Int = 3, minHope: Int = 1, maxHope: Int = 3, category: String? = null, sortBy: String = "latest", languages: List<String> = emptyList(), page: Int = 1, perPage: Int = 20, forceRefresh: Boolean = false): PaginatedSearchResponse
    suspend fun getRatedVideos(userToken: String, page: Int = 1, perPage: Int = 20, category: String? = null, sortBy: String = "latest"): RatedVideosResponse
    suspend fun blockUser(blockerUserToken: String, blockedUserToken: String): ApiResponse
    suspend fun unblockUser(blockerUserToken: String, blockedUserToken: String): ApiResponse
    suspend fun getBlockedUsers(userToken: String): BlockedUsersResponse
    suspend fun deleteVideo(videoId: String): ApiResponse
    suspend fun syncFavorites(userToken: String, favorites: List<FavoriteVideoDto>): SyncFavoritesResponse
    suspend fun reportVideo(videoId: String, userToken: String, reason: String = "inappropriate"): ApiResponse
    suspend fun submitCrashReport(crashReport: CrashReport): CrashReportResponse
    suspend fun submitBulkRatings(ratings: List<RatingRequest>, userToken: String): BulkRatingResponse
    fun close()


    // ... postojeće metode ...

    suspend fun getPrayerRequests(
        userToken: String,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean = false
    ): PrayerPageDto

    suspend fun createPrayerRequest(
        body: CreatePrayerRequestBody
    ): PrayerRequestDto

    suspend fun setPrayerPrayed(
        requestId: String,
        body: SetPrayerPrayedBody
    ): PrayerRequestDto

    suspend fun getPrayerEncouragements(
        requestId: String,
        userToken: String
    ): List<EncouragementDto>

    suspend fun addPrayerEncouragement(
        requestId: String,
        body: AddEncouragementBody
    ): EncouragementDto

    suspend fun deletePrayerRequest(
        requestId: String,
        adminToken: String
    ): ApiResponse

    suspend fun healthCheck(): HealthCheckResponse

}
