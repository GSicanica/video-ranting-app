package com.youtube.rating.android.viewmodel

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.youtube.rating.android.domain.usecase.HomeBrowseRulesUseCase
import com.youtube.rating.android.domain.usecase.HomeRandomRequestUseCase
import com.youtube.rating.android.domain.usecase.ShuffleVideosUseCase
import com.youtube.rating.android.data.OfflineRepository
import com.youtube.rating.android.storage.FavoritesGateway
import com.youtube.rating.android.domain.usecase.home.GetHomeCategoriesUseCase
import com.youtube.rating.android.domain.usecase.home.GetPopularSearchTermsUseCase
import com.youtube.rating.android.domain.usecase.home.GetTopVideosUseCase
import com.youtube.rating.android.domain.usecase.home.ReportVideoUseCase
import com.youtube.rating.android.domain.usecase.home.SearchVideosUseCase

/**
 * Factory for creating HomeViewModel with dependencies
 * Uses SavedStateHandle to persist richer UI state.
 */
class HomeViewModelFactory(
    private val getPopularSearchTermsUseCase: GetPopularSearchTermsUseCase,
    private val getHomeCategoriesUseCase: GetHomeCategoriesUseCase,
    private val getTopVideosUseCase: GetTopVideosUseCase,
    private val searchVideosUseCase: SearchVideosUseCase,
    private val reportVideoUseCase: ReportVideoUseCase,
    private val favoritesGateway: FavoritesGateway,
    private val offlineRepository: OfflineRepository,
    private val ratingApiClient: com.youtube.rating.shared.api.RatingApiClient,
    private val youTubeInfoService: com.youtube.rating.android.youtube.YouTubeInfoService,
    private val userTokenManager: com.youtube.rating.android.utils.UserTokenManager,
    private val owner: ComponentActivity,
    private val saintOfDayManager: com.youtube.rating.android.utils.SaintOfDayManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val application = owner.application
            val handle = extras.createSavedStateHandle()
            return HomeViewModel(
                application = application,
                getPopularSearchTermsUseCase = getPopularSearchTermsUseCase,
                getHomeCategoriesUseCase = getHomeCategoriesUseCase,
                getTopVideosUseCase = getTopVideosUseCase,
                searchVideosUseCase = searchVideosUseCase,
                reportVideoUseCase = reportVideoUseCase,
                favoritesGateway = favoritesGateway,
                offlineRepository = offlineRepository,
                ratingApiClient = ratingApiClient,
                youTubeInfoService = youTubeInfoService,
                userTokenManager = userTokenManager,
                savedStateHandle = handle,
                homeBrowseRulesUseCase = HomeBrowseRulesUseCase(),
                homeRandomRequestUseCase = HomeRandomRequestUseCase(),
                shuffleVideosUseCase = ShuffleVideosUseCase(),
                saintOfDayManager = saintOfDayManager
            ) as T
        }
        val e = IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        com.youtube.rating.android.sentry.SentryLogger.captureException(
            e,
            tags = mapOf("where" to "HomeViewModelFactory.create")
        )
        error(e.message ?: "Unknown ViewModel class")
    }
}
