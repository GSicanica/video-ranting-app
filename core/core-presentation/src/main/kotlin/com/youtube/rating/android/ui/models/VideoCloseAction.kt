package com.youtube.rating.android.ui.models

enum class VideoCloseAction(val prefValue: String) {
    MINI_PLAYER("mini_player");

    companion object {
        fun fromPref(value: String?): VideoCloseAction? {
            return when (value) {
                MINI_PLAYER.prefValue -> MINI_PLAYER
                else -> MINI_PLAYER
            }
        }
    }
}
