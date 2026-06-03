package com.youtube.rating.shared.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class VideoRating(
    val videoId: String,
    val videoTitle: String,
    val videoThumbnail: String,
    val videoChannel: String,
    val love: Int, // 1-3
    val faith: Int, // 1-3
    val hope: Int, // 1-3
    val deviceId: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class RatingRequest(
    val videoId: String? = null,          // YT id (nullable for some cases)
    val videoTitle: String,
    val videoThumbnail: String,
    val videoChannel: String = "",        // Non-nullable with default empty string to ensure it's always sent in JSON

    val platform: String = "youtube",     // "youtube" | "facebook"
    val videoUrl: String? = null,         // FB resolved url (ili raw)

    val love: Int,
    val faith: Int,
    val hope: Int,
    val category: String? = null,
    val userToken: String?,
    val language: String
)


@Serializable
data class RatingResponse(
    val success: Boolean,
    val message: String,
    val ratingId: String? = null
)

@Serializable
data class BulkRatingRequest(
    val userToken: String?,
    val ratings: List<RatingRequest>
)

@Serializable
data class BulkRatingResult(
    val index: Int,
    val videoId: String,
    val action: String, // "created" or "updated"
    val ratingId: String? = null,
    val success: Boolean,
    val error: String? = null
)

@Serializable
data class BulkRatingResponse(
    val success: Boolean,
    val message: String,
    val totalProcessed: Int,
    val successCount: Int,
    val errorCount: Int,
    val results: List<BulkRatingResult>
)

@Serializable
data class VideoStats(
    val videoId: String,
    val videoTitle: String,
    val videoThumbnail: String,
    val averageLove: Double,
    val averageFaith: Double,
    val averageHope: Double,
    val totalRatings: Int,
    val channelName: String? = null,
    val category: String? = null
)

@Serializable
data class RatedVideo(
    val videoId: String,
    val videoTitle: String,
    val channelName: String,
    val thumbnail: String,
    val category: String? = null,
    val myLove: Int,
    val myFaith: Int,
    val myHope: Int,
    val ratedAt: String,
    val totalRatings: Int,
    val avgLove: Double? = null,
    val avgFaith: Double? = null,
    val avgHope: Double? = null
)

@Serializable
data class RatedVideosResponse(
    val success: Boolean,
    val data: List<RatedVideo> = emptyList(),
    val pagination: PaginationInfo? = null
)

@Serializable
data class PaginationInfo(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total") val total: Int = 1,
)

@Serializable
data class GospelSection(
    val name: String? = null,
    val reference: String? = null,
    val title: String? = null,
    val text: String? = null
)

@Serializable
data class GospelDayResponse(
    val success: Boolean = false,
    val date: String? = null,
    val url: String? = null,
    val prvo_citanje: GospelSection? = null,
    val psalam: GospelSection? = null,
    val evandjelje: GospelSection? = null,
    val message: String? = null
)

@Serializable
data class RandomPassageResponse(
    val success: Boolean = false,
    val book: String? = null,
    val chapter: String? = null,
    val title: String? = null,
    val text: String? = null,
    val message: String? = null
)

@Serializable
data class BibleSearchResult(
    val book: String = "",
    val chapter: String = "",
    val snippet: String? = null
)

@Serializable
data class BibleSearchResponse(
    val success: Boolean = false,
    val query: String? = null,
    val count: Int = 0,
    val scanned: Int = 0,
    val results: List<BibleSearchResult> = emptyList(),
    val message: String? = null
)

@Serializable
data class NovenaActiveItem(
    val id: String = "",
    val name_hr: String? = null,
    val name_en: String? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val day_number: Int = 0,
    val how_hr: List<String> = emptyList(),
    val how_en: List<String> = emptyList()
)

@Serializable
data class NovenaUpcomingItem(
    val id: String = "",
    val name_hr: String? = null,
    val name_en: String? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val days_until_start: Int = 0
)

@Serializable
data class NovenaTodayResponse(
    val success: Boolean = false,
    val date: String? = null,
    val tz: String? = null,
    val active_today: List<NovenaActiveItem> = emptyList(),
    val upcoming: List<NovenaUpcomingItem> = emptyList(),
    val message: String? = null
)

@Serializable
data class YouTubeVideoInfo(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val language: String = "unknown"
)

@Serializable
data class VideoSearchResult(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnail: String,
    val createdAt: Long = 0L,
    val totalRatings: Int = 0,
    val avgLove: Double = 0.0,
    val avgFaith: Double = 0.0,
    val avgHope: Double = 0.0,
    val avgTotal: Double = 0.0,
    val category: String? = null,
    val language: String = "unknown",
    val whyTag: String? = null
)

@Serializable
data class SearchResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val videos: List<VideoSearchResult> = emptyList()
)

@Serializable
data class PaginatedSearchResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val total: Int = 0,
    val page: Int = 1,
    val perPage: Int = 20,
    val totalPages: Int = 0,
    val hasMore: Boolean = false,
    val videos: List<VideoSearchResult> = emptyList()
)

@Serializable
data class TopVideosResponse(
    val success: Boolean = false,
    val type: String = "",
    val title: String = "",
    val count: Int = 0,
    val videos: List<VideoSearchResult> = emptyList()
)

@Serializable
data class HomeCategory(
    val id: Int,
    val name: String,
    val value: String,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

@Serializable
data class HomeCategoriesResponse(
    val success: Boolean = false,
    val categories: List<HomeCategory> = emptyList(),
    val message: String? = null
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String? = null,
    val data: String? = null
)

// New enhanced API response models
@Serializable
data class VideosListResponse(
    val success: Boolean,
    val data: VideosData,
    val timestamp: Long
)

@Serializable
data class VideosData(
    val videos: List<VideoItem>,
    val pagination: PaginationData
)

@Serializable
data class VideoItem(
    val id: String,
    val title: String,
    val thumbnail: String,
    val channel_name: String? = null,
    val language: String? = null,
    val category: String? = null,
    val categories: List<String> = emptyList(),
    val display_order: Int = 0,
    val created_at: Long = 0
)

@Serializable
data class PaginationData(
    val current_page: Int,
    val per_page: Int,
    val total: Int,
    val total_pages: Int
)

@Serializable
data class SearchVideosResponse(
    val success: Boolean,
    val data: SearchData,
    val timestamp: Long
)

@Serializable
data class SearchData(
    val results: List<VideoItem>,
    val query: String,
    val pagination: PaginationData,
    val filters: SearchFilters? = null
)

@Serializable
data class SearchFilters(
    val category: String? = null,
    val language: String? = null
)

@Serializable
data class BlockedDevice(
    val deviceId: String,
    val blockedAt: Long
)

@Serializable
data class BlockedUsersResponse(
    val success: Boolean,
    val blockedDevices: List<BlockedDevice> = emptyList(),
    val count: Int = 0
)

@Serializable
data class FavoriteVideoDto(
    val videoId: String,
    val title: String,
    val thumbnail: String? = null,
    val channelName: String? = null,
    val avgLove: Double = 0.0,
    val avgFaith: Double = 0.0,
    val avgHope: Double = 0.0,
    val totalRatings: Int = 0,
    val category: String? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class SyncFavoritesRequest(
    val user_token: String,
    val favorites: List<FavoriteVideoDto>
)

@Serializable
data class SyncFavoritesResponse(
    val success: Boolean,
    val message: String,
    val favorites: List<FavoriteVideoDto> = emptyList(),
    val count: Int = 0
)

@Serializable
data class PsalmHighlightsSyncRequest(
    val userToken: String,
    val highlights: List<String> = emptyList(),
    val displayName: String? = null,
    val gender: String? = null,
    val notMarried: Boolean? = null
)

@Serializable
data class PsalmHighlightsSyncResponse(
    val success: Boolean,
    val message: String? = null,
    val highlightCount: Int = 0
)

@Serializable
data class PsalmRoomRequest(
    val userToken: String,
    val displayName: String,
    val gender: String,
    val notMarried: Boolean,
    val selectedTokens: List<String>,
    val favoritePsalm: String? = null
)

@Serializable
data class PsalmRoomResponse(
    val success: Boolean,
    val message: String? = null,
    val room: String? = null,
    val highlightCount: Int = 0
)

@Serializable
data class LiveKitTokenRequest(
    val userToken: String,
    val room: String,
    val identity: String,
    val name: String
)

@Serializable
data class LiveKitTokenResponse(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null,
    val url: String? = null
)

@Serializable
data class PsalmAvailabilitySetRequest(
    val userToken: String,
    val gender: String,
    val notMarried: Boolean,
    val favoritePsalm: String,
    val availableFrom: String,
    val displayName: String? = null,
    val ageYears: Int? = null
)

@Serializable
data class PsalmAvailabilitySetResponse(
    val success: Boolean,
    val message: String? = null,
    val favoritePsalm: String? = null,
    val gender: String? = null,
    val availableFrom: String? = null,
    val availableTo: String? = null
)

@Serializable
data class PsalmAvailabilityListRequest(
    val userToken: String,
    val gender: String,
    val favoritePsalm: String
)

@Serializable
data class PsalmAvailabilityItem(
    val displayName: String,
    val availableFrom: String? = null,
    val availableTo: String? = null
)

@Serializable
data class PsalmAvailabilityListResponse(
    val success: Boolean,
    val message: String? = null,
    val favoritePsalm: String? = null,
    val items: List<PsalmAvailabilityItem> = emptyList()
)

@Serializable
data class CrashReport(
    val stackTrace: String,
    val errorMessage: String? = null,
    val timestamp: Long,
    val appVersion: String,
    val appVersionCode: Int,
    val androidVersion: String,
    val androidSdk: Int,
    val deviceModel: String,
    val manufacturer: String,
    val userToken: String? = null,
    val threadName: String? = null,
    val availableMemory: Long? = null,
    val totalMemory: Long? = null,
    val customData: Map<String, String>? = null
)

@Serializable
data class CrashReportResponse(
    val success: Boolean,
    val message: String,
    val reportId: String? = null
)

// ==========================================
// STANDARDIZED API RESPONSE MODELS
// Matching the enhanced backend format
// ==========================================

/**
 * Standardized API Response wrapper for all endpoints
 * Matches the backend ApiResponse format
 */
@Serializable
data class StandardizedApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val code: Int? = null,
    val meta: ResponseMeta? = null
)

@kotlinx.serialization.Serializable
data class PopularTermDto(
    val term: String,
    val count: Int = 0,
    val language: String? = null
)

@kotlinx.serialization.Serializable
data class PopularTermsResponse(
    val success: Boolean,
    val message: String? = null,
    val terms: List<PopularTermDto> = emptyList()
)

/**
 * Response metadata for pagination and additional info
 */
@Serializable
data class ResponseMeta(
    val timestamp: Long? = null,
    val version: String? = null,
    val pagination: PaginationMeta? = null,
    val rateLimit: RateLimitInfo? = null
)

/**
 * Enhanced pagination metadata
 */
@Serializable
data class PaginationMeta(
    val current_page: Int,
    val total_pages: Int,
    val total_items: Int,
    val per_page: Int,
    val has_more: Boolean = false
)

/**
 * Rate limiting information from headers
 */
@Serializable
data class RateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val reset: Long
)

/**
 * Enhanced video list response with standardized format
 */
@Serializable
data class EnhancedVideosListResponse(
    val success: Boolean,
    val message: String,
    val data: EnhancedVideosData,
    val meta: ResponseMeta? = null
)

/**
 * Enhanced videos data structure
 */
@Serializable
data class EnhancedVideosData(
    val videos: List<EnhancedVideoItem>,
    val pagination: PaginationMeta
)

/**
 * Enhanced video item with all backend fields
 */
@Serializable
data class EnhancedVideoItem(
    val id: String,
    val youtube_id: String,
    val title: String,
    val channel_name: String,
    val avg_rating: Double,
    val total_ratings: Int,
    val language: String,
    val categories: List<String> = emptyList(),
    val created_at: Long
)

/**
 * Enhanced rating response
 */
@Serializable
data class EnhancedRatingResponse(
    val success: Boolean,
    val message: String,
    val data: RatingResultData? = null
)

/**
 * Rating result data
 */
@Serializable
data class RatingResultData(
    val rating_id: String,
    val new_average: Double,
    val total_ratings: Int
)

/**
 * Health check response
 */
@Serializable(with = HealthCheckResponseSerializer::class)
data class HealthCheckResponse(
    val success: Boolean,
    val message: String,
    val data: HealthData? = null,
    val meta: ResponseMeta? = null
)

/**
 * Backward/forward compatible deserializer for `/api/health.php`.
 *
 * - Legacy schema (wrapped): { success, message, data, meta }
 * - Current backend schema (raw): { status, timestamp, checks, ... }
 */
object HealthCheckResponseSerializer : KSerializer<HealthCheckResponse> {
    @Serializable
    private data class Wrapped(
        val success: Boolean,
        val message: String,
        val data: HealthData? = null,
        val meta: ResponseMeta? = null
    )

    override val descriptor: SerialDescriptor = Wrapped.serializer().descriptor

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): HealthCheckResponse {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("HealthCheckResponseSerializer supports only JSON")
        val element: JsonElement = jsonDecoder.decodeJsonElement()

        if (element is JsonObject && element.containsKey("success")) {
            return try {
                val wrapped = jsonDecoder.json.decodeFromJsonElement(Wrapped.serializer(), element)
                HealthCheckResponse(
                    success = wrapped.success,
                    message = wrapped.message,
                    data = wrapped.data,
                    meta = wrapped.meta
                )
            } catch (e: Exception) {
                throw SerializationException("Invalid wrapped health response", e)
            }
        }

        val raw = try {
            jsonDecoder.json.decodeFromJsonElement(HealthCheckRawResponse.serializer(), element)
        } catch (e: Exception) {
            return HealthCheckResponse(
                success = false,
                message = "Invalid health response schema",
                data = null,
                meta = null
            )
        }

        val isHealthy = raw.status == "healthy"
        val timestampMs = try {
            Instant.parse(raw.timestamp).toEpochMilliseconds()
        } catch (_: Exception) {
            Clock.System.now().toEpochMilliseconds()
        }

        val mappedChecks = raw.checks.mapValues { (_, item) ->
            HealthCheckItem(
                status = item.status,
                message = item.message,
                response_time = item.durationMs?.let { "${it}ms" },
                error_count = null
            )
        }

        return HealthCheckResponse(
            success = isHealthy,
            message = if (isHealthy) "OK" else "Backend unhealthy",
            data = HealthData(
                status = raw.status,
                version = raw.version ?: "unknown",
                timestamp = timestampMs,
                checks = mappedChecks,
                performance = null
            ),
            meta = null
        )
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: HealthCheckResponse) {
        val wrapped = Wrapped(
            success = value.success,
            message = value.message,
            data = value.data,
            meta = value.meta
        )
        Wrapped.serializer().serialize(encoder, wrapped)
    }
}

/**
 * Health check data
 */
@Serializable
data class HealthData(
    val status: String,
    val version: String,
    val timestamp: Long,
    val checks: Map<String, HealthCheckItem>,
    val performance: PerformanceMetrics? = null
)

/**
 * Individual health check item
 */
@Serializable
data class HealthCheckItem(
    val status: String,
    val message: String,
    val response_time: String? = null,
    val error_count: Int? = null
)

/**
 * Performance metrics
 */
@Serializable
data class PerformanceMetrics(
    val response_time: String,
    val memory_peak: String,
    val queries_executed: Int = 0
)

@Serializable
data class PsalmDto(
    val psalm: Int,
    val url: String,
    val title: String,
    val text: String
)

// ==========================================
// APP USAGE TRACKING MODELS
// ==========================================

/**
 * App usage session data
 */
@Serializable
data class AppUsageSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long = 0, // in milliseconds
    val deviceId: String,
    val platform: String, // "android" or "ios"
    val appVersion: String,
    val userToken: String? = null,
    val featuresUsed: List<String> = emptyList(), // list of features accessed
    val screenTime: Map<String, Long> = emptyMap() // screen name -> time spent
)

/**
 * Daily usage statistics
 */
@Serializable
data class DailyUsageStats(
    val date: String, // YYYY-MM-DD format
    val totalSessions: Int,
    val totalDuration: Long, // total time in milliseconds
    val averageSessionDuration: Long,
    val uniqueUsers: Int,
    val topFeatures: List<FeatureUsage> = emptyList(),
    val hourlyBreakdown: List<HourlyUsage>
)

/**
 * Feature usage data
 */
@Serializable
data class FeatureUsage(
    val featureName: String,
    val usageCount: Int,
    val totalTime: Long,
    val averageTime: Long
)

/**
 * Hourly usage breakdown
 */
@Serializable
data class HourlyUsage(
    val hour: Int, // 0-23
    val sessionCount: Int,
    val totalDuration: Long
)

/**
 * Weekly/Monthly usage summary
 */
@Serializable
data class UsageSummary(
    val period: String, // "week" or "month"
    val startDate: String,
    val endDate: String,
    val totalSessions: Int,
    val totalDuration: Long,
    val averageDailySessions: Double,
    val averageDailyDuration: Long,
    val mostActiveDay: String,
    val leastActiveDay: String,
    val retentionRate: Double? = null, // percentage of returning users
    val newUsers: Int,
    val returningUsers: Int
)

/**
 * Chart data point for graphs
 */
@Serializable
data class ChartDataPoint(
    val label: String,
    val value: Double,
    val timestamp: Long? = null,
    val category: String? = null
)

/**
 * Usage analytics response
 */
@Serializable
data class UsageAnalyticsResponse(
    val success: Boolean,
    val message: String,
    val data: UsageAnalyticsData? = null
)

/**
 * Usage analytics data
 */
@Serializable
data class UsageAnalyticsData(
    val summary: UsageSummary,
    val currentSessionTime: Long = 0,
    val dailyStats: List<DailyUsageStats>,
    val featureStats: List<FeatureUsage>,
    val tabUsageStats: List<TabUsageStats> = emptyList(),
    val chartData: List<ChartDataPoint>,
    val userInsights: UserInsights? = null
)

/**
 * Tab usage statistics
 */
@Serializable
data class TabUsageStats(
    val tabName: String,
    val totalTime: Long, // total time spent in seconds
    val visitCount: Int, // number of visits to this tab
    val averageTime: Long // average time per visit in seconds
)

/**
 * User insights and patterns
 */
@Serializable
data class UserInsights(
    val averageSessionLength: Long,
    val mostUsedFeatures: List<String>,
    val peakUsageHours: List<Int>,
    val userRetention: Double,
    val featureAdoption: Map<String, Double> // feature -> adoption percentage
)

/**
 * App usage tracking request
 */
@Serializable
data class TrackUsageRequest(
    val sessionId: String,
    val action: String, // "start", "end", "feature_used", "screen_view"
    val timestamp: Long,
    val data: Map<String, String> = emptyMap() // additional context data
)

/**
 * Watch history requests
 */
@Serializable
data class WatchHistoryRecordRequest(
    val userToken: String,
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val category: String = "",
    val source: String,
    val watchDuration: Int,
    val totalDuration: Int
)

@Serializable
data class WatchHistoryToggleRequest(
    val userToken: String,
    val enabled: Boolean
)

@Serializable
data class WatchHistoryClearRequest(
    val userToken: String
)

@Serializable
data class BugReportRequest(
    val title: String,
    val description: String,
    val device_info: Map<String, String>,
    val app_version: String,
    val user_token: String? = null,
    val user_email: String? = null,
    val priority: String = "medium"
)

@Serializable
data class AnonymousRegisterResponse(
    val userToken: String
)

@Serializable
data class GalleryImageDto(
    val id: String,
    val url: String,
    val name: String? = null,
    val size: Long? = null,
    val modifiedAt: Long? = null
)

@Serializable
data class GalleryListResponse(
    val success: Boolean,
    val message: String? = null,
    val count: Int = 0,
    val images: List<GalleryImageDto> = emptyList()
)
