package com.youtube.rating.running.data.di

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import com.youtube.rating.android.feature.running.data.RunningLegacyPrefsMigration
import com.youtube.rating.android.feature.running.data.RunningPrefsDataStore
import com.youtube.rating.android.feature.running.data.RunningRepositoryImpl
import com.youtube.rating.android.feature.running.domain.RouteTracker
import com.youtube.rating.android.feature.running.domain.RunningRepository
import com.youtube.rating.android.feature.running.domain.RunningTrackingServiceController
import com.youtube.rating.android.feature.running.location.AndroidRouteTracker
import com.youtube.rating.android.feature.running.service.RunningTrackingServiceControllerImpl
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

val runningDataModule = module {
    single {
        RunningPrefsDataStore(
            dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() }),
                migrations = listOf(RunningLegacyPrefsMigration(androidContext())),
                scope = get(named("appIoScope")),
                produceFile = {
                    File(androidContext().filesDir, "datastore/running_prefs.preferences_pb")
                },
            ),
        )
    }

    single<RunningRepository> {
        RunningRepositoryImpl(
            dataStoreHolder = get(),
            json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }

    single<RouteTracker> { AndroidRouteTracker(context = androidContext()) }

    single<RunningTrackingServiceController> {
        RunningTrackingServiceControllerImpl(context = androidContext())
    }
}
