package com.youtube.rating.shared.data

import kotlinx.serialization.Serializable

/**
 * Domain models (not Realm objects) - KMP style
 */
data class NoteModel(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val lastModified: Long
)

data class FavoriteVideoModel(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val avgLove: Double,
    val avgFaith: Double,
    val avgHope: Double,
    val totalRatings: Int,
    val category: String?,
    val timestamp: Long,
    val type: FavoriteItemTypeModels = FavoriteItemTypeModels.VIDEO
)

data class OfflineVideoModel(
    val id: String,
    val youtubeId: String?,
    val title: String,
    val channelName: String,
    val localPath: String,
    val thumbnailPath: String?,
    val thumbnailUrl: String?,
    val duration: Long,
    val fileSize: Long,
    val category: String?,
    val addedAt: Long
)

data class HomeScreenCacheModel(
    val cacheKey: String,
    val searchQuery: String,
    val category: String?,
    val minLove: Int,
    val maxLove: Int,
    val minFaith: Int,
    val maxFaith: Int,
    val minHope: Int,
    val maxHope: Int,
    val languages: List<String>,
    val sortBy: String?,
    val page: Int,
    val perPage: Int,
    val responseData: String, // JSON string of PaginatedSearchResponse
    val timestamp: Long,
    val expiresAt: Long,
    val accessCount: Int,
    val lastAccessed: Long
)

enum class FavoriteItemTypeModels {
    VIDEO,
    PSALM,
    PSALM_HIGHLIGHTED,
    OTHER
}

@Serializable
data class CreatePrayerRequestBody(
    val userToken: String?,
    val text: String,
    val authorName: String?
)


@Serializable
data class SetPrayerPrayedBody(
    val userToken: String?,
    val prayed: Boolean
)

@Serializable
data class SetPrayerPrayedRequest(
    val requestId: String,
    val userToken: String?,
    val prayed: Boolean
)


@Serializable
data class AddEncouragementBody(
    val userToken: String?,
    val message: String,
    val authorName: String?,
    val text: String = ""
)


// -------- DTO responses --------

@Serializable
data class PrayerPageDto(
    val items: List<PrayerRequestDto>,
    val hasMore: Boolean
)


@Serializable
data class EncouragementDto(
    val id: String,
    val requestId: String,
    val authorName: String?,
    val message: String,
    val createdAtEpochMs: Long
)

@Serializable
data class PrayerRequestsPageDto(
    val items: List<PrayerRequestDto>,
    val hasMore: Boolean
)

@Serializable
data class PrayerRequestDto(
    val id: String,
    val authorName: String? = null,
    val text: String,
    val createdAtEpochMs: Long,
    val prayedCount: Int,
    val encouragementCount: Int,
    val iPrayed: Boolean,
    val tags: List<String> = emptyList()
)


@Serializable
data class PrayerEncouragementDto(
    val id: String,
    val requestId: String,
    val authorName: String? = null,
    val message: String,
    val createdAtEpochMs: Long
)
