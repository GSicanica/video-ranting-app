package com.youtube.rating.shared.data

/**
 * Sealed class for data operation results - KMP style
 */
sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : DataResult<Nothing>()
    data object Loading : DataResult<Nothing>()
}

/**
 * Sealed class for Note states
 */
sealed class NoteState {
    data object Idle : NoteState()
    data object Loading : NoteState()
    data class Success(val notes: List<NoteModel>) : NoteState()
    data class Error(val message: String) : NoteState()
}

/**
 * Sealed class for Note events
 */
sealed class NoteEvent {
    data class AddNote(val title: String, val content: String) : NoteEvent()
    data class UpdateNote(val id: String, val title: String, val content: String) : NoteEvent()
    data class DeleteNote(val id: String) : NoteEvent()
    data object LoadNotes : NoteEvent()
    data object ClearAll : NoteEvent()
}

/**
 * Sealed class for Favorite states
 */
sealed class FavoriteState {
    data object Idle : FavoriteState()
    data object Loading : FavoriteState()
    data class Success(val favorites: List<FavoriteVideoModel>) : FavoriteState()
    data class Error(val message: String) : FavoriteState()
}

/**
 * Sealed class for Favorite events
 */
sealed class FavoriteEvent {
    data class AddFavorite(val video: FavoriteVideoModel) : FavoriteEvent()
    data class RemoveFavorite(val videoId: String) : FavoriteEvent()
    data class IsFavorite(val videoId: String) : FavoriteEvent()
    data object LoadFavorites : FavoriteEvent()
    data object ClearAll : FavoriteEvent()
}

/**
 * Sealed class for OfflineVideo states
 */
sealed class OfflineVideoState {
    data object Idle : OfflineVideoState()
    data object Loading : OfflineVideoState()
    data class Success(val videos: List<OfflineVideoModel>) : OfflineVideoState()
    data class Error(val message: String) : OfflineVideoState()
}

/**
 * Sealed class for OfflineVideo events
 */
sealed class OfflineVideoEvent {
    data class AddVideo(val video: OfflineVideoModel) : OfflineVideoEvent()
    data class RemoveVideo(val id: String) : OfflineVideoEvent()
    data class UpdateVideo(val id: String, val title: String?, val category: String?) : OfflineVideoEvent()
    data object LoadVideos : OfflineVideoEvent()
    data object ClearAll : OfflineVideoEvent()
}
