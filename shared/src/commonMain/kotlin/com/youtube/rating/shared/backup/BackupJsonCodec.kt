package com.youtube.rating.shared.backup

import com.youtube.rating.shared.data.FavoriteVideoModel
import com.youtube.rating.shared.data.NoteModel
import com.youtube.rating.shared.data.OfflineVideoModel

expect object BackupJsonCodec {
    fun encodeNotes(notes: List<NoteModel>): String
    fun decodeNotes(jsonArrayString: String): List<NoteModel>

    fun encodeFavorites(favorites: List<FavoriteVideoModel>): String
    fun decodeFavorites(jsonArrayString: String): List<FavoriteVideoModel>

    fun encodeOfflineVideos(videos: List<OfflineVideoModel>): String
    fun decodeOfflineVideos(jsonArrayString: String): List<OfflineVideoModel>
}
