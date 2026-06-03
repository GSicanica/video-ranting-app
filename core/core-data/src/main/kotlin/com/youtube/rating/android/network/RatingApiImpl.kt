package com.youtube.rating.android.network

import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.data.AddEncouragementBody
import com.youtube.rating.shared.data.CreatePrayerRequestBody
import com.youtube.rating.shared.data.EncouragementDto
import com.youtube.rating.shared.data.PrayerPageDto
import com.youtube.rating.shared.data.PrayerRequestDto
import com.youtube.rating.shared.data.SetPrayerPrayedBody
import com.youtube.rating.shared.models.*
import com.youtube.rating.shared.utils.Logger

class RatingApiImpl(private val client: RatingApiClient) : RatingApi {
    private suspend fun safeApi(call: suspend () -> ApiResponse): ApiResponse =
        runCatching { call() }.getOrElse { ApiResponse(success = false, message = it.toFriendlyMessage()) }

    private fun Throwable.toFriendlyMessage(): String {
        val name = this::class.simpleName ?: ""
        return when {
            this is java.net.UnknownHostException -> "Nema interneta. Provjeri vezu."
            name.contains("ConnectTimeout", ignoreCase = true) ||
                    name.contains("HttpRequestTimeout", ignoreCase = true) -> "Veza je istekla. Pokušaj ponovo."
            name.contains("ResponseException", ignoreCase = true) ||
                    name.contains("ClientRequestException", ignoreCase = true) ||
                    name.contains("ServerResponseException", ignoreCase = true) -> "Greška na serveru: ${message ?: name}"
            else -> message ?: "Neočekivana greška."
        }
    }

    override suspend fun submitRating(rating: RatingRequest): RatingResponse = client.submitRating(rating)
    override suspend fun getVideoStats(videoId: String): VideoStats = client.getVideoStats(videoId)
    override suspend fun getAllVideos(): List<VideoStats> = client.getAllVideos()
    override suspend fun getVideosList(page: Int, limit: Int, category: String?, language: String?): VideosListResponse = client.getVideosList(page, limit, category, language)
    override suspend fun searchVideosNew(query: String, page: Int, limit: Int, category: String?, language: String?): SearchVideosResponse = client.searchVideosNew(query, page, limit, category, language)
    override suspend fun getYouTubeVideoInfo(videoId: String): YouTubeVideoInfo = client.getYouTubeVideoInfo(videoId)
    override suspend fun searchVideos(searchQuery: String?, minLove: Int, minFaith: Int, minHope: Int, category: String?, sortBy: String, languages: List<String>): List<VideoSearchResult> = client.searchVideos(searchQuery, minLove, minFaith, minHope, category, sortBy, languages)
    override suspend fun searchVideosV2(searchQuery: String?, minLove: Int, maxLove: Int, minFaith: Int, maxFaith: Int, minHope: Int, maxHope: Int, category: String?, sortBy: String, languages: List<String>, page: Int, perPage: Int, forceRefresh: Boolean): PaginatedSearchResponse =
        client.searchVideosV2(searchQuery, minLove, maxLove, minFaith, maxFaith, minHope, maxHope, category, sortBy, languages, page, perPage, forceRefresh)
    override suspend fun getRatedVideos(userToken: String, page: Int, perPage: Int, category: String?, sortBy: String): RatedVideosResponse = client.getRatedVideos(userToken, page, perPage, category, sortBy)
    override suspend fun blockUser(blockerUserToken: String, blockedUserToken: String): ApiResponse =
        safeApi { client.blockUser(blockerUserToken, blockedUserToken) }

    override suspend fun unblockUser(blockerUserToken: String, blockedUserToken: String): ApiResponse =
        safeApi { client.unblockUser(blockerUserToken, blockedUserToken) }
    override suspend fun getBlockedUsers(userToken: String): BlockedUsersResponse = client.getBlockedUsers(userToken)
    override suspend fun deleteVideo(videoId: String): ApiResponse =
        safeApi { client.deleteVideo(videoId) }
    override suspend fun syncFavorites(userToken: String, favorites: List<FavoriteVideoDto>): SyncFavoritesResponse = client.syncFavorites(userToken, favorites)
    override suspend fun reportVideo(videoId: String, userToken: String, reason: String): ApiResponse =
        safeApi { client.reportVideo(videoId, userToken, reason) }

    override suspend fun submitCrashReport(crashReport: CrashReport): CrashReportResponse = client.submitCrashReport(crashReport)
    override suspend fun submitBulkRatings(ratings: List<RatingRequest>, userToken: String): BulkRatingResponse = client.submitBulkRatings(ratings, userToken)
    override fun close() = client.close()



    override suspend fun getPrayerRequests(
        userToken: String,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean
    ): PrayerPageDto =
        client.getPrayerRequests(userToken, page, pageSize, forceRefresh)

    override suspend fun createPrayerRequest(body: CreatePrayerRequestBody): PrayerRequestDto =
        client.createPrayerRequest(body)

    override suspend fun setPrayerPrayed(requestId: String, body: SetPrayerPrayedBody): PrayerRequestDto {
        Logger.info("RatingApi", "setPrayerPrayed requestId=$requestId prayed=${body.prayed}")
        return client.setPrayerPrayed(requestId, body)
    }

    override suspend fun getPrayerEncouragements(requestId: String, userToken: String): List<EncouragementDto> =
        client.getPrayerEncouragements(requestId, userToken)

    override suspend fun addPrayerEncouragement(requestId: String, body: AddEncouragementBody): EncouragementDto =
        client.addPrayerEncouragement(requestId, body)

    override suspend fun deletePrayerRequest(requestId: String, adminToken: String): ApiResponse =
        safeApi { client.deletePrayerRequest(requestId, adminToken) }

    override suspend fun healthCheck(): HealthCheckResponse =
        client.healthCheck()
}
