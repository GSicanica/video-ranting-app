package com.youtube.rating.running.presentation.di

import com.youtube.rating.android.feature.running.viewmodel.RunningViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private val runningPresentationModule = module {
    viewModel {
        RunningViewModel(
            repository = get(),
            routeTracker = get(),
            trackingServiceController = get(),
        )
    }
}

val runningFeatureModules = listOf(
    runningPresentationModule,
)
