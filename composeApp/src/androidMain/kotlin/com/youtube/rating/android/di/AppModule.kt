package com.youtube.rating.android.di

import com.youtube.rating.android.data.repository.PsalmsRepository
import com.youtube.rating.android.viewmodel.PsalmsViewModel
import com.youtube.rating.habittracker.feature.habits.data.di.habitsDataModule
import com.youtube.rating.habittracker.presentation.di.habitTrackerFeatureModules
import com.youtube.rating.running.data.di.runningDataModule
import com.youtube.rating.running.presentation.di.runningFeatureModules
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * App-level Koin assembly.
 *
 * Feature and infrastructure bindings live in smaller modules so this file stays readable.
 */
val appModule = module {
    includes(
        appCoreModule,
        backupUseCaseModule,
        repositoryModule,
        appManagerModule,
        appViewModelModule,
        watchHistoryModule,
    )
}

val psalmsModule = module {
    single { PsalmsRepository(context = get()) }
    viewModel { PsalmsViewModel(repo = get()) }
}

val allModules = listOf(
    appModule,
    psalmsModule,
) + habitsDataModule + habitTrackerFeatureModules + runningDataModule + runningFeatureModules
