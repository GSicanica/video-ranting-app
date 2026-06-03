package com.youtube.rating.android.di

import com.youtube.rating.android.data.FavoritesRepository
import com.youtube.rating.android.data.FavoritesRepositoryImpl
import com.youtube.rating.android.data.OfflineRepository
import com.youtube.rating.android.data.OfflineRepositoryImpl
import com.youtube.rating.android.data.repository.HomeContentRepository
import com.youtube.rating.android.data.repository.HomeContentRepositoryImpl
import com.youtube.rating.android.data.repository.RatingsRepository
import com.youtube.rating.android.data.repository.RatingsRepositoryImpl
import com.youtube.rating.android.data.settings.SettingsRepository
import com.youtube.rating.android.data.settings.SettingsRepositoryImpl
import com.youtube.rating.android.domain.repository.HomeContentGateway
import com.youtube.rating.android.domain.repository.RatingsGateway
import com.youtube.rating.android.domain.usecase.home.GetHomeCategoriesUseCase
import com.youtube.rating.android.domain.usecase.home.GetPopularSearchTermsUseCase
import com.youtube.rating.android.domain.usecase.home.GetTopVideosUseCase
import com.youtube.rating.android.domain.usecase.home.ReportVideoUseCase
import com.youtube.rating.android.domain.usecase.home.SearchVideosUseCase
import com.youtube.rating.android.domain.usecase.ratings.BlockUserUseCase
import com.youtube.rating.android.domain.usecase.ratings.DeleteRatingUseCase
import com.youtube.rating.android.domain.usecase.ratings.SubmitRatingUseCase
import com.youtube.rating.android.domain.usecase.ratings.UnblockUserUseCase
import com.youtube.rating.android.domain.usecase.ratings.UpdateRatingUseCase
import com.youtube.rating.android.repository.PrayerRepository
import com.youtube.rating.android.repository.PrayerRepositoryImpl
import com.youtube.rating.android.source.HomeContentSource
import com.youtube.rating.android.source.HomeContentSourceRegistry
import com.youtube.rating.android.source.RatingApiHomeContentSource
import com.youtube.rating.shared.data.HomeScreenCacheRepository
import com.youtube.rating.shared.data.NotesRepository
import com.youtube.rating.shared.data.OfflineVideosRepository
import com.youtube.rating.shared.data.RepositoryFactory
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule = module {
    single<PrayerRepository> {
        PrayerRepositoryImpl(
            api = get(),
            userTokenManager = get(),
        )
    }

    single<FavoritesRepository> { FavoritesRepositoryImpl(get()) }

    single<NotesRepository> {
        get<RepositoryFactory>().createNotesRepository()
    }
    single<NotesRepository>(qualifier = named("sharedNotesRepo")) {
        get<NotesRepository>()
    }

    single<com.youtube.rating.shared.data.FavoritesRepository> {
        get<RepositoryFactory>().createFavoritesRepository()
    }
    single<com.youtube.rating.shared.data.FavoritesRepository>(qualifier = named("sharedFavoritesRepo")) {
        get<com.youtube.rating.shared.data.FavoritesRepository>()
    }

    single<OfflineVideosRepository> {
        get<RepositoryFactory>().createOfflineVideosRepository()
    }
    single<OfflineVideosRepository>(qualifier = named("sharedOfflineVideosRepo")) {
        get<OfflineVideosRepository>()
    }

    single<HomeScreenCacheRepository> {
        get<RepositoryFactory>().createHomeScreenCacheRepository()
    }
    single<HomeScreenCacheRepository>(qualifier = named("sharedHomeCacheRepo")) {
        get<HomeScreenCacheRepository>()
    }
    factory {
        com.youtube.rating.android.cache.HomeScreenCacheManager(
            persistentRepository = get(qualifier = named("sharedHomeCacheRepo")),
        )
    }

    single<HomeContentSource> { RatingApiHomeContentSource(apiClient = get()) }
    single { HomeContentSourceRegistry(sources = listOf(get())) }
    single<HomeContentRepository> { HomeContentRepositoryImpl(sourceRegistry = get()) }
    single<HomeContentGateway> { get<HomeContentRepository>() }

    single { GetPopularSearchTermsUseCase(repo = get()) }
    single { GetHomeCategoriesUseCase(repo = get()) }
    single { GetTopVideosUseCase(repo = get()) }
    single { SearchVideosUseCase(repo = get()) }
    single { ReportVideoUseCase(repo = get()) }

    single<RatingsRepository> { RatingsRepositoryImpl(apiClient = get()) }
    single<RatingsGateway> { get<RatingsRepository>() }
    single { SubmitRatingUseCase(repo = get()) }
    single { UpdateRatingUseCase(repo = get()) }
    single { DeleteRatingUseCase(repo = get()) }
    single { BlockUserUseCase(repo = get()) }
    single { UnblockUserUseCase(repo = get()) }

    single<SettingsRepository> { SettingsRepositoryImpl(appContext = androidApplication()) }
    single<OfflineRepository> { OfflineRepositoryImpl(factory = get()) }

    single {
        com.youtube.rating.android.data.repository.CallsRepository(get())
    }

    single<com.youtube.rating.android.data.WatchHistoryRepository> {
        com.youtube.rating.android.data.WatchHistoryRepository(get())
    }
}
