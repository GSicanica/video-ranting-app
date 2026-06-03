package com.youtube.rating.android.ui.screens

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import com.youtube.rating.android.data.OfflineRepository
import com.youtube.rating.android.data.WatchHistoryRepository
import com.youtube.rating.android.domain.usecase.home.GetHomeCategoriesUseCase
import com.youtube.rating.android.domain.usecase.home.GetPopularSearchTermsUseCase
import com.youtube.rating.android.domain.usecase.home.GetTopVideosUseCase
import com.youtube.rating.android.domain.usecase.home.ReportVideoUseCase
import com.youtube.rating.android.domain.usecase.home.SearchVideosUseCase
import com.youtube.rating.android.storage.FavoritesGateway
import com.youtube.rating.android.utils.AnalyticsManager
import com.youtube.rating.android.utils.SaintOfDayManager
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.viewmodel.HomeViewModelFactory
import com.youtube.rating.android.viewmodel.RatingViewModelFactory
import com.youtube.rating.android.youtube.YouTubeInfoService
import com.youtube.rating.shared.api.RatingApiClient
import org.koin.compose.koinInject

internal data class HomeScreenDependencies(
    val favoritesGateway: FavoritesGateway,
    val offlineRepository: OfflineRepository,
    val youTubeInfoService: YouTubeInfoService,
    val apiClient: RatingApiClient,
    val getPopularSearchTermsUseCase: GetPopularSearchTermsUseCase,
    val getHomeCategoriesUseCase: GetHomeCategoriesUseCase,
    val getTopVideosUseCase: GetTopVideosUseCase,
    val searchVideosUseCase: SearchVideosUseCase,
    val reportVideoUseCase: ReportVideoUseCase,
    val userTokenManager: UserTokenManager,
    val analyticsManager: AnalyticsManager,
    val watchHistoryRepository: WatchHistoryRepository,
    val saintOfDayManager: SaintOfDayManager
) {
    fun ratingViewModelFactory(application: Application): RatingViewModelFactory =
        RatingViewModelFactory(
            application = application,
            apiClient = apiClient,
            youTubeInfoService = youTubeInfoService,
            userTokenManager = userTokenManager,
            analyticsManager = analyticsManager,
            watchHistoryRepository = watchHistoryRepository
        )

    fun homeViewModelFactory(owner: ComponentActivity): HomeViewModelFactory =
        HomeViewModelFactory(
            getPopularSearchTermsUseCase = getPopularSearchTermsUseCase,
            getHomeCategoriesUseCase = getHomeCategoriesUseCase,
            getTopVideosUseCase = getTopVideosUseCase,
            searchVideosUseCase = searchVideosUseCase,
            reportVideoUseCase = reportVideoUseCase,
            favoritesGateway = favoritesGateway,
            offlineRepository = offlineRepository,
            ratingApiClient = apiClient,
            youTubeInfoService = youTubeInfoService,
            userTokenManager = userTokenManager,
            owner = owner,
            saintOfDayManager = saintOfDayManager
        )
}

@Composable
internal fun rememberHomeScreenDependencies(): HomeScreenDependencies =
    HomeScreenDependencies(
        favoritesGateway = koinInject(),
        offlineRepository = koinInject(),
        youTubeInfoService = koinInject(),
        apiClient = koinInject(),
        getPopularSearchTermsUseCase = koinInject(),
        getHomeCategoriesUseCase = koinInject(),
        getTopVideosUseCase = koinInject(),
        searchVideosUseCase = koinInject(),
        reportVideoUseCase = koinInject(),
        userTokenManager = koinInject(),
        analyticsManager = koinInject(),
        watchHistoryRepository = koinInject(),
        saintOfDayManager = koinInject()
    )
