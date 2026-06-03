package com.youtube.rating.shared.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Notes - KMP style
 */
interface NotesRepository {
    fun getAllNotes(): Flow<List<NoteModel>>
    suspend fun addNote(title: String, content: String): DataResult<NoteModel>
    suspend fun importNote(note: NoteModel): DataResult<NoteModel>
    suspend fun updateNote(id: String, title: String, content: String): DataResult<NoteModel>
    suspend fun deleteNote(id: String): DataResult<Unit>
    suspend fun clearAll(): DataResult<Unit>
}

/**
 * Repository interface for Favorites - KMP style
 */
interface FavoritesRepository {
    fun getAllFavorites(): Flow<List<FavoriteVideoModel>>
    suspend fun addFavorite(video: FavoriteVideoModel): DataResult<Unit>
    suspend fun removeFavorite(videoId: String): DataResult<Unit>
    suspend fun updateFavoriteCategory(videoId: String, category: String?): DataResult<Unit>
    suspend fun isFavorite(videoId: String): Boolean
    suspend fun clearAll(): DataResult<Unit>
}

/**
 * Repository interface for Offline Videos - KMP style
 */
interface OfflineVideosRepository {
    fun getAllVideos(): Flow<List<OfflineVideoModel>>
    suspend fun addVideo(video: OfflineVideoModel): DataResult<OfflineVideoModel>
    suspend fun updateVideo(id: String, title: String?, category: String?): DataResult<Unit>
    suspend fun deleteVideo(id: String): DataResult<Unit>
    suspend fun clearAll(): DataResult<Unit>
}

/**
 * Repository interface for Home Screen Cache - KMP style
 */
interface HomeScreenCacheRepository {
    suspend fun getCachedResponse(cacheKey: String): HomeScreenCacheModel?
    suspend fun saveCachedResponse(model: HomeScreenCacheModel): DataResult<Unit>
    suspend fun deleteExpiredEntries(): DataResult<Int>
    suspend fun clearAll(): DataResult<Unit>
    suspend fun getCacheStats(): CacheStats
    suspend fun updateAccessStats(cacheKey: String): DataResult<Unit>
}

/**
 * Simple cache statistics for monitoring
 */
data class CacheStats(
    val totalEntries: Int,
    val validEntries: Int,
    val expiredEntries: Int,
    val maxSize: Int,
    val ttlMs: Long
) {
    val utilizationPercent: Double
        get() = if (maxSize > 0) (totalEntries * 100.0) / maxSize else 0.0

    override fun toString(): String = """
        Cache Stats:
        - Total Entries: $totalEntries
        - Valid Entries: $validEntries
        - Expired Entries: $expiredEntries
        - Utilization: ${utilizationPercent.toInt()}%
        - Max Size: $maxSize
        - TTL: ${ttlMs / 1000}s
    """.trimIndent()
}
