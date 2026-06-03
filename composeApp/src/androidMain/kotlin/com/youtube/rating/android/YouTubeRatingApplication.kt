package com.youtube.rating.android

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.youtube.rating.android.data.PrefsDataStore
import com.youtube.rating.android.di.RatingWorkerFactory
import com.youtube.rating.android.di.allModules
import com.youtube.rating.android.migration.MigrationResult
import com.youtube.rating.android.sentry.SentryBreadcrumbs
import com.youtube.rating.android.sentry.SentryCoroutineExceptionHandler
import com.youtube.rating.android.sentry.SentryInitializer
import com.youtube.rating.android.sentry.SentryLogListener
import com.youtube.rating.android.sentry.SentryLogger
import com.youtube.rating.android.sentry.SentryNetworkInterceptor
import com.youtube.rating.android.sentry.SentryUserContextProvider
import com.youtube.rating.android.storage.FastingManager
import com.youtube.rating.android.storage.FavoritesManager
import com.youtube.rating.android.utils.AnalyticsManager
import com.youtube.rating.android.utils.AppScope
import com.youtube.rating.android.utils.AutoBackupManager
import com.youtube.rating.android.utils.BibleReminderScheduler
import com.youtube.rating.android.utils.CrashReportHandler
import com.youtube.rating.android.utils.FastingReminderScheduler
import com.youtube.rating.android.utils.MemoryProfiler
import com.youtube.rating.android.utils.PerformanceProfile
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.shared.api.AndroidCacheDirProvider
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.data.RealmProvider
import com.youtube.rating.shared.data.initializeAndroidRealmEncryption
import com.youtube.rating.shared.l10n.L10n
import com.youtube.rating.shared.utils.LogConfig
import com.youtube.rating.shared.utils.LogLevel
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.java.KoinJavaComponent.inject
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import com.youtube.rating.android.data.prefs.AdminPrefs
import com.youtube.rating.android.data.prefs.AppUsagePrefs
import com.youtube.rating.android.data.prefs.InstallPrefs
import com.youtube.rating.android.domain.usecase.AutoBackupIfNeededUseCase
import com.youtube.rating.android.domain.usecase.MigrationUseCase
import com.youtube.rating.android.domain.usecase.SyncNewVideoNotificationsUseCase
import com.youtube.rating.android.ui.components.clearVideoPlayerWebViewPool
import com.youtube.rating.core.coroutines.ioDispatcher

/**
 * Application class for YouTube Rating App.
 */
class YouTubeRatingApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    // NOTE: Do not resolve Koin dependencies until after startKoin() in onCreate()
    private val apiClient: RatingApiClient by inject(RatingApiClient::class.java)
    val userTokenManager: UserTokenManager by inject(UserTokenManager::class.java)
    private val analyticsManager: AnalyticsManager by inject(AnalyticsManager::class.java)
    private val autoBackupManager: AutoBackupManager by inject(AutoBackupManager::class.java)
    private val migrationUseCase: MigrationUseCase by inject(MigrationUseCase::class.java)
    private val autoBackupIfNeededUseCase: AutoBackupIfNeededUseCase by inject(AutoBackupIfNeededUseCase::class.java)
    private val syncNewVideoNotificationsUseCase: SyncNewVideoNotificationsUseCase by inject(
        SyncNewVideoNotificationsUseCase::class.java
    )
    private val biblePlannerManager: com.youtube.rating.android.storage.BiblePlannerManager by inject(
        com.youtube.rating.android.storage.BiblePlannerManager::class.java
    )

    // App usage time tracking
    private var appStartTime: Long = 0L

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + SentryCoroutineExceptionHandler)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(RatingWorkerFactory())
            .build()

    private fun launchIo(block: suspend CoroutineScope.() -> Unit) {
        appScope.launch(ioDispatcher, block = block)
    }

    private var lifecycleObserver: DefaultLifecycleObserver? = null
    private val deferredInitStarted = AtomicBoolean(false)

    companion object {
        @Volatile
        private var INSTANCE: YouTubeRatingApplication? = null

        fun getInstanceOrNull(): YouTubeRatingApplication? = INSTANCE

        fun getInstance(): YouTubeRatingApplication {
            val inst = INSTANCE
            if (inst != null) return inst

            val e = IllegalStateException("Application not initialized yet")
            SentryLogger.captureException(
                e,
                tags = mapOf("where" to "YouTubeRatingApplication.getInstance")
            )
            return checkNotNull(INSTANCE) { e.message ?: "Application not initialized yet" }
        }
    }

    override fun onCreate() {
        super.onCreate()
        L10n.init(this)

        // 1) Shared cacheDir provider
        AndroidCacheDirProvider.cacheDir = cacheDir
        initializeAndroidRealmEncryption(context = this)

        // 3) Logging config early
        val logsEnabled = BuildConfig.ENABLE_LOGGING || BuildConfig.DEBUG
        LogConfig.ENABLE_LOGS = logsEnabled
        Logger.isEnabled = logsEnabled
        Logger.minLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO

        if (LogConfig.ENABLE_LOGS) {
            LogConfig.log("✅ LogConfig initialized - ENABLE_LOGS = ${LogConfig.ENABLE_LOGS}")
            Logger.info("Application", "YouTubeRatingApplication started")
        }

        INSTANCE = this
        // 4) PrefsDataStore
        PrefsDataStore.initialize(this)

        // 4.5) Base URL provider (must initialize before Koin)
        com.youtube.rating.android.network.BaseUrlProvider.initialize(this)

        // 5) Start Koin (must happen before any injected dependency is used)
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@YouTubeRatingApplication)
            modules(allModules)
        }

        Logger.info("Application", "Koin DI initialized - All dependencies ready")

        // 6) Process lifecycle observer for usage tracking
        lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                appStartTime = System.currentTimeMillis()
                if (deferredInitStarted.get()) {
                    analyticsManager.onAppForeground()
                }
                if (LogConfig.ENABLE_LOGS) Logger.debug("Application", "App came to foreground")
            }

            override fun onStop(owner: LifecycleOwner) {
                if (appStartTime > 0L) {
                    val sessionTime = System.currentTimeMillis() - appStartTime
                    // Report session duration to Sentry (distribution metric).
                    SentryLogger.metricDistribution("app_session_ms", sessionTime.toDouble())
                    appScope.launch(ioDispatcher) {
                        val token = userTokenManager.getUserTokenAsync()
                        if (!token.isNullOrBlank()) {
                            AppUsagePrefs.addAppUsageTimeMs(this@YouTubeRatingApplication, sessionTime, token)
                        }
                    }
                    if (LogConfig.ENABLE_LOGS) {
                        Logger.debug("Application", "App background, session: ${sessionTime}ms")
                    }
                }
                if (deferredInitStarted.get()) {
                    analyticsManager.onAppBackground()
                }
                appStartTime = 0L
            }
        }
        lifecycleObserver?.let { ProcessLifecycleOwner.get().lifecycle.addObserver(it) }

        // 7) Crash reporter
        installCrashReporter()

        // 8) Defer heavy init until first Activity resumed
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                startDeferredInitOnce()
                unregisterActivityLifecycleCallbacks(this)
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

	    private fun startDeferredInitOnce() {
	        if (!deferredInitStarted.compareAndSet(false, true)) return
	        postFirstFrame {
	            launchIo {
	                // Debug-only: force one sampled transaction so Sentry shows the first profile.
	                // Run it here to avoid impacting cold start.
	                runCatching { SentryInitializer.debugProfileProbe() }

	                // Sentry init can do I/O; keep it off the main thread.
	                runCatching {
	                    SentryInitializer.init(this@YouTubeRatingApplication)
	                    Logger.addListener(SentryLogListener())
	                    SentryBreadcrumbs.action("app_start")
	                    SentryLogger.metricCount("app_start", 1.0)
	                }

	                // Warm up Realm / repos
	                runCatching {
	                    RealmProvider.getInstance()
	                }

	                // Update Sentry context
	                runCatching {
	                    val cachedToken = userTokenManager.getCachedUserToken()
	                    val installId = InstallPrefs.getInstallId(this@YouTubeRatingApplication)
	                    val installIdHash = installId?.takeIf { it.isNotBlank() }
	                        ?.let { SentryUserContextProvider.hashForTag(it) }
	                    val userIdSource = when {
	                        !cachedToken.isNullOrBlank() -> "token"
	                        !installId.isNullOrBlank() -> "installId"
	                        else -> "none"
	                    }

	                    SentryUserContextProvider.setUserContext(
	                        token = cachedToken,
	                        installId = installId
	                    )
	                    SentryLogger.updateContext(
	                        tags = mapOf(
	                            "loggingEnabled" to LogConfig.ENABLE_LOGS.toString(),
	                            "baseUrl" to com.youtube.rating.shared.BASE_URL,
	                            "useLocalServer" to BuildConfig.USE_LOCAL_SERVER.toString(),
	                            "debugUnlocked" to BuildConfig.DEBUG.toString(),
	                            "userTokenPresent" to (!cachedToken.isNullOrBlank()).toString(),
	                            "installIdHash" to (installIdHash ?: "none"),
	                            "userIdSource" to userIdSource
	                        )
	                    )
	                }

	                // Release-only debug flags
	                if (!BuildConfig.DEBUG) {
	                    runCatching {
	                        val debugUnlocked = AdminPrefs.getDebugUnlocked(this@YouTubeRatingApplication)
	                        val releaseLogsEnabled = AdminPrefs.getReleaseLogsEnabled(this@YouTubeRatingApplication)

	                        if (debugUnlocked || releaseLogsEnabled) {
	                            LogConfig.ENABLE_LOGS = true
	                            Logger.isEnabled = true
	                            Logger.minLevel = LogLevel.DEBUG
	                        } else {
	                            Logger.minLevel = LogLevel.INFO
	                        }

	                        SentryLogger.updateContext(
	                            tags = mapOf(
	                                "loggingEnabled" to LogConfig.ENABLE_LOGS.toString(),
	                                "debugUnlocked" to debugUnlocked.toString(),
	                                "releaseLogsEnabled" to releaseLogsEnabled.toString()
	                            )
	                        )
	                    }
	                }

	                // Migration + backup
	                runCatching {
	                    when (val result = migrationUseCase()) {
	                        is MigrationResult.Success -> Logger.info("Migration", "Completed: ${result.stats}")
	                        is MigrationResult.AlreadyMigrated -> Logger.debug("Migration", "Already migrated")
	                        is MigrationResult.Failed -> Logger.error("Migration", "Failed: ${result.error}")
	                    }
	                    autoBackupIfNeededUseCase()
	                }

	                // Periodic tasks / reminders
	                runCatching {
	                    com.youtube.rating.android.workers.WorkManagerHelper.schedulePeriodicTasks(
	                        this@YouTubeRatingApplication
	                    )
	                }
	                runCatching {
	                    syncNewVideoNotificationsUseCase()
	                }
	                runCatching {
	                    val fastingManager = FastingManager.getInstance(this@YouTubeRatingApplication)
	                    FastingReminderScheduler.schedule(
	                        this@YouTubeRatingApplication,
	                        fastingManager.getReminderSettings()
	                    )
	                }
	                runCatching {
	                    BibleReminderScheduler.schedule(
	                        this@YouTubeRatingApplication,
	                        biblePlannerManager.getReminderSettings()
	                    )
	                }

	                // Anonymous token (best effort)
	                runCatching {
	                    if (!userTokenManager.hasUserToken()) {
	                        val response = apiClient.anonymousRegister()
	                        userTokenManager.setUserProvidedToken(response.userToken)
	                        SentryUserContextProvider.setUserToken(response.userToken)
	                    }
	                }

	                // Send pending crash reports
	                runCatching {
	                    CrashReportHandler.sendPendingReports(
	                        this@YouTubeRatingApplication,
	                        apiClient,
	                        userTokenManager
	                    )
	                }

	                // Refresh Base URL from server (best effort)
	                runCatching {
	                    com.youtube.rating.android.network.BaseUrlProvider.refreshFromServer(
	                        this@YouTubeRatingApplication
	                    )
	                }

	                // Trigger an initial foreground event once deferred init is ready.
	                Handler(Looper.getMainLooper()).post {
	                    runCatching { analyticsManager.onAppForeground() }
	                }
	            }
	        }
	    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            clearVideoPlayerWebViewPool(destroy = true)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        clearVideoPlayerWebViewPool(destroy = true)
    }

    private fun postFirstFrame(block: () -> Unit) {
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().postFrameCallback { block() }
        }
    }

    private fun installCrashReporter() {
        try {
            Logger.info("CrashReporter", "Installing CrashReportHandler")
            CrashReportHandler.install(
                context = this,
                apiClient = apiClient,
                userTokenManager = userTokenManager
            )
            Logger.info("CrashReporter", "Global crash handler installed")
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("CrashReporter", "Failed to install crash handler", e)
        }
    }

    override fun onTerminate() {
        lifecycleObserver?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
        lifecycleObserver = null

        super.onTerminate()

        analyticsManager.forceEndSession()

        try {
            autoBackupManager.cleanup()
            MemoryProfiler.cleanup()
            CrashReportHandler.cleanup()

            runCatching {
                val favoritesManager: FavoritesManager by inject(FavoritesManager::class.java)
                favoritesManager.cleanup()
            }.onFailure {
                Logger.warning("Application", "Failed to cleanup FavoritesManager: ${it.message}")
            }

            Logger.info("Application", "Resources cleaned up successfully")
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("Application", "Error during cleanup", e)
        }

        appScope.cancel()
        AppScope.cancel()
        RealmProvider.close()
        apiClient.close()
    }

    // Coil ImageLoader optimized
    override fun newImageLoader(): ImageLoader {
        val allowedHosts = setOfNotNull(parseHost(url = com.youtube.rating.shared.BASE_URL))
        val perfProfile = PerformanceProfile.get(this)

        val okHttpClient = OkHttpClient.Builder()
            .connectionPool(
                ConnectionPool(
                    maxIdleConnections = 15,
                    keepAliveDuration = 10,
                    TimeUnit.MINUTES
                )
            )
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = perfProfile.maxImageRequests
                    maxRequestsPerHost = perfProfile.maxImageRequestsPerHost
                }
            )
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(SentryNetworkInterceptor(allowedHosts = allowedHosts))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "YouTubeRating/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()

        val isLowRam = perfProfile.deviceClass == PerformanceProfile.DeviceClass.LOW
        val memoryCachePercent = if (isLowRam) 0.15 else 0.25
        val diskCacheBytes = if (isLowRam) {
            minOf(perfProfile.imageDiskCacheBytes, 40L * 1024L * 1024L)
        } else {
            perfProfile.imageDiskCacheBytes
        }

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            // Cap memory cache to ~25% of available heap; keeps decoding fast without evict storms
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(diskCacheBytes)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(false)
            .allowHardware(!isLowRam)
            .allowRgb565(isLowRam)
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .apply {
                if (BuildConfig.ENABLE_LOGGING) logger(DebugLogger())
            }
            .build()
    }

    private fun parseHost(url: String): String? {
        return try {
            URI(url).host?.lowercase()
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }
}
