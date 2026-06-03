package com.youtube.rating.shared.backup

import com.youtube.rating.shared.data.FavoriteVideoModel
import com.youtube.rating.shared.data.NoteModel
import com.youtube.rating.shared.data.OfflineVideoModel

actual object BackupJsonCodec {
    actual fun encodeNotes(notes: List<NoteModel>): String = "[]"
    actual fun decodeNotes(jsonArrayString: String): List<NoteModel> = emptyList()

    actual fun encodeFavorites(favorites: List<FavoriteVideoModel>): String = "[]"
    actual fun decodeFavorites(jsonArrayString: String): List<FavoriteVideoModel> = emptyList()

    actual fun encodeOfflineVideos(videos: List<OfflineVideoModel>): String = "[]"
    actual fun decodeOfflineVideos(jsonArrayString: String): List<OfflineVideoModel> = emptyList()
}
