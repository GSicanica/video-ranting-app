package com.youtube.rating.android.core

object AppKeys {
    object IntentExtras {
        const val OPEN_SCREEN = "extra_open_screen"
    }

    object OpenScreens {
        const val GOSPEL_DAY = "gospel_day"
        const val TRAINING = "training"
        const val HABIT_TRACKER = "habit_tracker"
    }

    object SavedState {
        object Home {
            const val UI_STATE = "home_ui_state"
            const val BROWSE_FILTERS = "browse_rating_filters"
            const val SELECTED_CATEGORY = "selected_category"
            const val PSALM_REFRESH = "psalm_refresh"
        }
    }

    object Prefs {
        const val EXTERNAL_STREAMS_JSON = "external_streams_json"
    }

    object Clipboard {
        const val DEBUG_SERVER_LOGS = "debug_server_logs"
        const val DEBUG_LAST_STATUS = "debug_last_status"
    }
}
