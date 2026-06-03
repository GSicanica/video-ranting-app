package com.youtube.rating.shared.data

import io.realm.kotlin.ext.toRealmList
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Realm model for user notes
 */
class NoteEntity : RealmObject {
    @PrimaryKey
    var id: String = ""
    var title: String = ""
    var content: String = ""
    var timestamp: Long = 0L
    var lastModified: Long = 0L
}

/**
 * Realm model for favorite videos
 */
class FavoriteVideoEntity : RealmObject {
    @PrimaryKey
    var videoId: String = ""
    var title: String = ""
    var thumbnail: String = ""
    var channelName: String = ""
    var avgLove: Double = 0.0
    var avgFaith: Double = 0.0
    var avgHope: Double = 0.0
    var totalRatings: Int = 0
    var category: String = ""
    var timestamp: Long = 0L
    var type: String = "VIDEO"
}

/**
 * Realm model for offline videos
 */
class OfflineVideoEntity : RealmObject {
    @PrimaryKey
    var id: String = ""
    var youtubeId: String = ""
    var title: String = ""
    var channelName: String = ""
    var localPath: String = ""
    var thumbnailPath: String = ""
    var thumbnailUrl: String = ""
    var duration: Long = 0L
    var fileSize: Long = 0L
    var category: String = ""
    var addedAt: Long = 0L
}

/**
 * Realm model for app data (usage time, settings, etc.)
 */
class AppDataEntity : RealmObject {
    @PrimaryKey
    var id: String = "app_data" // Singleton entity
    var appUsageTimeMs: Long = 0L
    var lastUpdated: Long = 0L
}

/**
 * Realm model for cached home screen data
 */
class HomeScreenCacheEntity : RealmObject {
    @PrimaryKey
    var cacheKey: String = "" // Composite key for cache identification
    var searchQuery: String = ""
    var category: String = ""
    var minLove: Int = 1
    var maxLove: Int = 3
    var minFaith: Int = 1
    var maxFaith: Int = 3
    var minHope: Int = 1
    var maxHope: Int = 3
    var languages: String = "" // JSON array of language codes
    var sortBy: String = ""
    var page: Int = 1
    var perPage: Int = 20
    var responseData: String = "" // JSON string of PaginatedSearchResponse
    var timestamp: Long = 0L
    var expiresAt: Long = 0L
    var accessCount: Int = 0
    var lastAccessed: Long = 0L
}
