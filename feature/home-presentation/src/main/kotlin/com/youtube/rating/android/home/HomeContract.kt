package com.youtube.rating.android.home

import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.ui.models.BrowseRatingFilters
import com.youtube.rating.android.ui.models.HomeTab

internal object HomeContract {
    sealed interface Intent {
        data class SearchQueryChanged(val query: String) : Intent
        data object ClearSearch : Intent
        data class SortSelected(val sort: String?) : Intent
        data class ToggleLanguage(val language: Strings.Language) : Intent
        data class SelectPopularRange(val range: PopularRange) : Intent
        data class RefreshBrowse(
            val forceRefresh: Boolean = false,
            val preserveOrder: Boolean = false,
        ) : Intent

        data object LoadMore : Intent
        data class SetBrowseRatingFilters(val filters: BrowseRatingFilters) : Intent
        data class SelectTab(val tab: HomeTab) : Intent
    }

    sealed interface Effect {
        data class ShowToast(val message: String) : Effect
    }
}
