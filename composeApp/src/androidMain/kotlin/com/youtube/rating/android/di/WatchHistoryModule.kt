package com.youtube.rating.android.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val watchHistoryModule = module {
    single {
        com.youtube.rating.watchhistory.data.WatchHistoryManager.getInstance(androidContext())
    }

    single<com.youtube.rating.watchhistory.domain.WatchHistoryStore> {
        get<com.youtube.rating.watchhistory.data.WatchHistoryManager>()
    }

    single<com.youtube.rating.watchhistory.domain.WatchHistoryTokenProvider> {
        object : com.youtube.rating.watchhistory.domain.WatchHistoryTokenProvider {
            private val userTokenManager: com.youtube.rating.android.utils.UserTokenManager = get()

            override suspend fun getUserToken(): String? {
                return userTokenManager.getUserTokenAsync()
                    ?: userTokenManager.getCachedUserToken()
            }
        }
    }

    single {
        com.youtube.rating.watchhistory.data.WatchHistoryRepository(
            tokenProvider = get(),
            apiClient = get(),
        )
    }

    single<com.youtube.rating.watchhistory.domain.WatchHistoryRemoteRepository> {
        get<com.youtube.rating.watchhistory.data.WatchHistoryRepository>()
    }
}
