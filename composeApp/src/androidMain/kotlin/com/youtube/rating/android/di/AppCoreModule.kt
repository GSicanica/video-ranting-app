package com.youtube.rating.android.di

import com.youtube.rating.android.BuildConfig
import com.youtube.rating.android.data.prefs.InstallPrefs
import com.youtube.rating.android.network.HealthChecker
import com.youtube.rating.android.network.RatingApi
import com.youtube.rating.android.network.RatingApiImpl
import com.youtube.rating.android.util.UIConstants
import com.youtube.rating.android.utils.AppScope
import com.youtube.rating.android.youtube.YouTubeInfoCache
import com.youtube.rating.android.youtube.YouTubeInfoService
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.data.RepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appCoreModule = module {
    single {
        RatingApiClient(
            enableDebugLogging = BuildConfig.ENABLE_LOGGING,
            context = androidContext(),
        )
    }

    single<RatingApi> { RatingApiImpl(get()) }
    single { HealthChecker(get()) }

    single { AppScope.get() }
    single(qualifier = named("appIoScope")) { get<CoroutineScope>() }

    single {
        YouTubeInfoCache(
            context = androidContext(),
            ttlMs = UIConstants.YT_INFO_CACHE_TTL_MS,
        )
    }

    single {
        YouTubeInfoService(
            apiClient = get(),
            cache = get(),
        )
    }

    single(qualifier = named("installId")) {
        val installId = com.youtube.rating.shared.utils.AndroidDeviceIdGenerator.generateInstallId(androidContext())
        get<CoroutineScope>(named("appIoScope")).launch {
            runCatching { InstallPrefs.setInstallId(androidContext(), installId) }
        }
        installId
    }

    single {
        com.youtube.rating.android.utils.UserTokenManager(androidContext(), get())
    }

    single { RepositoryFactory }
}
