package com.youtube.rating.android.sentry

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.utils.DeviceInfoHelper
import com.youtube.rating.android.utils.AppScope
import com.youtube.rating.core.packageutils.installerPackageName
import com.youtube.rating.core.packageutils.isFromGooglePlay
import com.youtube.rating.core.root.isRootAvailable
import com.youtube.rating.core.root.isRooted
import io.sentry.Hint
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.Request
import io.sentry.SpanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URI
import java.util.Locale

object SentryInitializer {

    /**
     * Init Sentry manually (without relying on InitProvider / manifest parsing).
     * Call this as early as possible in Application.onCreate().
     */
    fun init(app: Application) {
        SentryAndroid.init(app) { options ->
            // Debug / diagnostics
            options.isDebug = BuildConfig.DEBUG
            if (BuildConfig.DEBUG) {
                options.setDiagnosticLevel(SentryLevel.DEBUG)
            }

            // Environment / release
            options.environment = resolveEnvironment(app = app)
            options.release =
                "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"

            // DSN (explicit)
            val dsn = BuildConfig.SENTRY_DSN.ifBlank { readStringMeta(app = app, key = "io.sentry.dsn").orEmpty() }
            if (dsn.isNotBlank()) {
                options.dsn = dsn
            }

            // Scrub sensitive data
            options.setBeforeSend { event: SentryEvent, _: Hint ->
                scrubEvent(event = event)
            }

            // Tracing + Profiling sample rates (profiles attach to sampled transactions)
            val defaultTraces = if (BuildConfig.DEBUG) 1.0 else 0.1
            val defaultProfiles = if (BuildConfig.DEBUG) 1.0 else 0.1
            val tracesFromManifest = readDoubleMeta(app = app, key = "io.sentry.traces.sample-rate")
            val profilesFromManifest = readDoubleMeta(app = app, key = "io.sentry.traces.profiling.session-sample-rate")
            options.tracesSampleRate = tracesFromManifest ?: defaultTraces
            options.profilesSampleRate = profilesFromManifest ?: defaultProfiles

            // Optional: useful defaults
            // options.isSendDefaultPii = false
            // options.isAttachThreads = true
        }

        // Attach base tags / context
        Sentry.configureScope { scope ->
            scope.setTag("buildType", BuildConfig.BUILD_TYPE)
            scope.setTag("versionName", BuildConfig.VERSION_NAME)
            scope.setTag("versionCode", BuildConfig.VERSION_CODE.toString())
            scope.setTag("locale", Locale.getDefault().toString())
            scope.setTag("deviceManufacturer", Build.MANUFACTURER)
            scope.setTag("deviceModel", Build.MODEL)
            scope.setTag("sdkInt", Build.VERSION.SDK_INT.toString())
            scope.setTag("isLowRamDevice", DeviceInfoHelper.isLowRamDevice(app).toString())
            scope.setTag("isRooted", isRooted().toString())
            scope.setTag("isRootAvailable", isRootAvailable().toString())
            scope.setTag("installer", (app.installerPackageName ?: "unknown"))
            scope.setTag("isFromGooglePlay", app.isFromGooglePlay.toString())
        }
    }

    /**
     * Debug-only: create a quick sampled transaction to force the first profile to show in Sentry UI.
     * Call after the first Activity resume (so it doesn't slow cold start).
     */
    fun debugProfileProbe() {
        if (!BuildConfig.DEBUG) return

        try {
            val tx = Sentry.startTransaction("profile-probe", "startup.probe")

            // Finish asynchronously; do not block main thread.
            AppScope.get().launch(Dispatchers.Default) {
                try {
                    delay(600) // enough time to capture something
                    tx.status = SpanStatus.OK
                } catch (t: Throwable) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(t)
                    tx.throwable = t
                    tx.status = SpanStatus.INTERNAL_ERROR
                } finally {
                    tx.finish()
                }
            }
        } catch (e: Throwable) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // best-effort
        }
    }

    private fun resolveEnvironment(app: Application): String {
        val manifestEnv = try {
            val appInfo = if (Build.VERSION.SDK_INT >= 33) {
                app.packageManager.getApplicationInfo(
                    app.packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
            }
            appInfo.metaData?.getString("io.sentry.environment")
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }

        return manifestEnv?.takeIf { it.isNotBlank() }
            ?: if (BuildConfig.DEBUG) "debug" else "release"
    }

    private fun readDoubleMeta(app: Application, key: String): Double? {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= 33) {
                app.packageManager.getApplicationInfo(
                    app.packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
            }
            val value = appInfo.metaData?.get(key) ?: return null
            when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }

    private fun readStringMeta(app: Application, key: String): String? {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= 33) {
                app.packageManager.getApplicationInfo(
                    app.packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
            }
            appInfo.metaData?.getString(key)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }

    /**
     * Remove tokens/PII from outgoing events (best-effort).
     */
    private fun scrubEvent(event: SentryEvent): SentryEvent {
        // Scrub request headers/body
        val request: Request? = event.request
        if (request != null) {
            // IMPORTANT: headers can be immutable -> always copy to mutable map
            val headers = request.headers?.toMutableMap() ?: mutableMapOf()

            headers.remove("Authorization")
            headers.remove("authorization")
            headers.remove("X-User-Token")
            headers.remove("x-user-token")

            request.headers = headers

            // Avoid sending raw request bodies
            request.data = null
            event.request = request
        }

        // Scrub breadcrumb data (breadcrumbs are separate from event.request).
        try {
            event.breadcrumbs?.forEach { b ->
                val data = b.data
                val sensitiveKeys = listOf(
                    "Authorization", "authorization",
                    "X-User-Token", "x-user-token"
                )

                sensitiveKeys.forEach { k ->
                    if (data.containsKey(k)) {
                        b.setData(k, "[Filtered]")
                    }
                }

                val url = data["url"] as? String
                if (!url.isNullOrBlank()) {
                    b.setData("url", sanitizeUrl(rawUrl = url))
                }
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // best-effort
        }

        // Scrub user (keep only id if you set it elsewhere)
        event.user?.let { user ->
            user.email = null
            user.username = null
            event.user = user
        }

        return event
    }

    private fun sanitizeUrl(rawUrl: String): String {
        return try {
            val uri = URI(rawUrl)
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            when {
                host.isNotBlank() && path.isNotBlank() -> "$host$path"
                path.isNotBlank() -> path
                else -> rawUrl.substringBefore('?').substringBefore('#')
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            rawUrl.substringBefore('?').substringBefore('#')
        }
    }
}
