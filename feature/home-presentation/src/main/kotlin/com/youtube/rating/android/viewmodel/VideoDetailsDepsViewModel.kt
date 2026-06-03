package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import com.youtube.rating.android.data.WatchHistoryRepository
import com.youtube.rating.android.utils.AnalyticsManager
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.youtube.YouTubeInfoService
import com.youtube.rating.shared.api.RatingApiClient

class VideoDetailsDepsViewModel(
    val apiClient: RatingApiClient,
    val userTokenManager: UserTokenManager,
    val analyticsManager: AnalyticsManager,
    val youTubeInfoService: YouTubeInfoService,
    val watchHistoryRepository: WatchHistoryRepository
) : ViewModel()
