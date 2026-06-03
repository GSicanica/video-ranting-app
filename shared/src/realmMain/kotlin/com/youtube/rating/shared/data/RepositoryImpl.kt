package com.youtube.rating.shared.data

import io.realm.kotlin.Realm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Generate unique ID for entities - KMP compatible
 */
private fun generateUniqueId(): String {
    val timestamp = Clock.System.now().toEpochMilliseconds()
    val randomPart = Random.nextInt(10000, 99999)
    return "$timestamp-$randomPart"
}

/**
 * Get current timestamp in milliseconds - KMP compatible
 */
private fun currentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

class NotesRepositoryImpl(private val realm: Realm) : NotesRepository {
    private val notesState = MutableStateFlow<List<NoteModel>>(emptyList())

    override fun getAllNotes(): Flow<List<NoteModel>> {
        return notesState.map { list -> list.sortedByDescending { it.lastModified } }
    }

    override suspend fun addNote(title: String, content: String): DataResult<NoteModel> {
        return try {
            val now = currentTimeMillis()
            val note = NoteModel(
                id = generateUniqueId(),
                title = title,
                content = content,
                timestamp = now,
                lastModified = now
            )
            notesState.value = notesState.value + note
            DataResult.Success(note)
        } catch (e: Exception) {
            DataResult.Error("Failed to add note", e)
        }
    }

    override suspend fun importNote(note: NoteModel): DataResult<NoteModel> {
        return try {
            val mutable = notesState.value.toMutableList()
            val index = mutable.indexOfFirst { it.id == note.id }
            if (index >= 0) {
                mutable[index] = note
            } else {
                mutable.add(note)
            }
            notesState.value = mutable
            DataResult.Success(note)
        } catch (e: Exception) {
            DataResult.Error("Failed to import note", e)
        }
    }

    override suspend fun updateNote(id: String, title: String, content: String): DataResult<NoteModel> {
        return try {
            val mutable = notesState.value.toMutableList()
            val index = mutable.indexOfFirst { it.id == id }
            if (index < 0) {
                return DataResult.Error("Note not found")
            }
            val updated = mutable[index].copy(
                title = title,
                content = content,
                lastModified = currentTimeMillis()
            )
            mutable[index] = updated
            notesState.value = mutable
            DataResult.Success(updated)
        } catch (e: Exception) {
            DataResult.Error("Failed to update note", e)
        }
    }

    override suspend fun deleteNote(id: String): DataResult<Unit> {
        return try {
            notesState.value = notesState.value.filterNot { it.id == id }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to delete note", e)
        }
    }

    override suspend fun clearAll(): DataResult<Unit> {
        return try {
            notesState.value = emptyList()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to clear notes", e)
        }
    }
}

class FavoritesRepositoryImpl(private val realm: Realm) : FavoritesRepository {
    private val favoritesState = MutableStateFlow(loadFavoritesFromRealm())

    override fun getAllFavorites(): Flow<List<FavoriteVideoModel>> {
        return favoritesState.map { list -> list.sortedByDescending { it.timestamp } }
    }

    override suspend fun addFavorite(video: FavoriteVideoModel): DataResult<Unit> {
        return try {
            val exists = realm.query(FavoriteVideoEntity::class, "videoId == $0", video.videoId)
                .find()
                .isNotEmpty()
            if (exists) return DataResult.Error("Video already in favorites")
            realm.writeBlocking {
                copyToRealm(video.toEntity())
            }
            favoritesState.value = loadFavoritesFromRealm()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to add favorite", e)
        }
    }

    override suspend fun removeFavorite(videoId: String): DataResult<Unit> {
        return try {
            realm.writeBlocking {
                query(FavoriteVideoEntity::class, "videoId == $0", videoId)
                    .find()
                    .forEach { entity -> delete(entity) }
            }
            favoritesState.value = loadFavoritesFromRealm()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to remove favorite", e)
        }
    }

    override suspend fun updateFavoriteCategory(videoId: String, category: String?): DataResult<Unit> {
        return try {
            val normalized = category?.takeIf { it.isNotBlank() }
            var updated = false
            realm.writeBlocking {
                val entity = query(FavoriteVideoEntity::class, "videoId == $0", videoId)
                    .find()
                    .firstOrNull()
                if (entity != null) {
                    entity.category = normalized.orEmpty()
                    updated = true
                }
            }
            if (!updated) return DataResult.Error("Favorite not found")
            favoritesState.value = loadFavoritesFromRealm()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to update favorite category", e)
        }
    }

    override suspend fun isFavorite(videoId: String): Boolean {
        return try {
            realm.query(FavoriteVideoEntity::class, "videoId == $0", videoId)
                .find()
                .isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun clearAll(): DataResult<Unit> {
        return try {
            realm.writeBlocking {
                query(FavoriteVideoEntity::class)
                    .find()
                    .forEach { entity -> delete(entity) }
            }
            favoritesState.value = emptyList()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to clear favorites", e)
        }
    }

    private fun loadFavoritesFromRealm(): List<FavoriteVideoModel> {
        return realm.query(FavoriteVideoEntity::class)
            .find()
            .map { entity -> entity.toModel() }
            .sortedByDescending { it.timestamp }
    }

    private fun FavoriteVideoEntity.toModel(): FavoriteVideoModel {
        return FavoriteVideoModel(
            videoId = videoId,
            title = title,
            thumbnail = thumbnail,
            channelName = channelName,
            avgLove = avgLove,
            avgFaith = avgFaith,
            avgHope = avgHope,
            totalRatings = totalRatings,
            category = category.takeIf { it.isNotBlank() },
            timestamp = timestamp,
            type = runCatching { FavoriteItemTypeModels.valueOf(type) }
                .getOrDefault(FavoriteItemTypeModels.VIDEO),
        )
    }

    private fun FavoriteVideoModel.toEntity(): FavoriteVideoEntity {
        return FavoriteVideoEntity().apply {
            videoId = this@toEntity.videoId
            title = this@toEntity.title
            thumbnail = this@toEntity.thumbnail
            channelName = this@toEntity.channelName
            avgLove = this@toEntity.avgLove
            avgFaith = this@toEntity.avgFaith
            avgHope = this@toEntity.avgHope
            totalRatings = this@toEntity.totalRatings
            category = this@toEntity.category.orEmpty()
            timestamp = this@toEntity.timestamp
            type = this@toEntity.type.name
        }
    }
}

class OfflineVideosRepositoryImpl(private val realm: Realm) : OfflineVideosRepository {
    private val videosState = MutableStateFlow<List<OfflineVideoModel>>(emptyList())

    override fun getAllVideos(): Flow<List<OfflineVideoModel>> {
        return videosState.map { list -> list.sortedByDescending { it.addedAt } }
    }

    override suspend fun addVideo(video: OfflineVideoModel): DataResult<OfflineVideoModel> {
        return try {
            videosState.value = videosState.value + video
            DataResult.Success(video)
        } catch (e: Exception) {
            DataResult.Error("Failed to add video", e)
        }
    }

    override suspend fun updateVideo(id: String, title: String?, category: String?): DataResult<Unit> {
        return try {
            val mutable = videosState.value.toMutableList()
            val index = mutable.indexOfFirst { it.id == id }
            if (index < 0) return DataResult.Error("Video not found")
            val current = mutable[index]
            mutable[index] = current.copy(
                title = title ?: current.title,
                category = category ?: current.category
            )
            videosState.value = mutable
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to update video", e)
        }
    }

    override suspend fun deleteVideo(id: String): DataResult<Unit> {
        return try {
            videosState.value = videosState.value.filterNot { it.id == id }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to delete video", e)
        }
    }

    override suspend fun clearAll(): DataResult<Unit> {
        return try {
            videosState.value = emptyList()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to clear videos", e)
        }
    }
}

class HomeScreenCacheRepositoryImpl(private val realm: Realm) : HomeScreenCacheRepository {
    private val cacheState = MutableStateFlow<Map<String, HomeScreenCacheModel>>(emptyMap())

    override suspend fun getCachedResponse(cacheKey: String): HomeScreenCacheModel? {
        val now = currentTimeMillis()
        val model = cacheState.value[cacheKey] ?: return null
        if (model.expiresAt <= now) {
            cacheState.value = cacheState.value - cacheKey
            return null
        }
        updateAccessStats(cacheKey)
        return model
    }

    override suspend fun saveCachedResponse(model: HomeScreenCacheModel): DataResult<Unit> {
        return try {
            cacheState.value = cacheState.value + (model.cacheKey to model)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to save cached response", e)
        }
    }

    override suspend fun deleteExpiredEntries(): DataResult<Int> {
        return try {
            val now = currentTimeMillis()
            val before = cacheState.value
            val filtered = before.filterValues { it.expiresAt > now }
            cacheState.value = filtered
            DataResult.Success(before.size - filtered.size)
        } catch (e: Exception) {
            DataResult.Error("Failed to delete expired entries", e)
        }
    }

    override suspend fun clearAll(): DataResult<Unit> {
        return try {
            cacheState.value = emptyMap()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("Failed to clear cache", e)
        }
    }

    override suspend fun getCacheStats(): CacheStats {
        val values = cacheState.value.values
        val now = currentTimeMillis()
        val totalEntries = values.size
        val expiredEntries = values.count { it.expiresAt <= now }
        val validEntries = totalEntries - expiredEntries
        return CacheStats(
            totalEntries = totalEntries,
            validEntries = validEntries,
            expiredEntries = expiredEntries,
            maxSize = 500,
            ttlMs = 60L * 60L * 1000L
        )
    }

    override suspend fun updateAccessStats(cacheKey: String): DataResult<Unit> {
        val existing = cacheState.value[cacheKey] ?: return DataResult.Success(Unit)
        val updated = existing.copy(
            accessCount = existing.accessCount + 1,
            lastAccessed = currentTimeMillis()
        )
        cacheState.value = cacheState.value + (cacheKey to updated)
        return DataResult.Success(Unit)
    }
}
