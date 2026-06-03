package com.youtube.rating.shared.l10n

import android.content.Context
import android.content.res.Configuration
import java.util.IllegalFormatException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

actual object PlatformStrings {
    private val appContextRef = AtomicReference<Context?>(null)
    private val stringIdCache = ConcurrentHashMap<String, Int>()

    actual fun init(platformContext: Any?) {
        val ctx = platformContext as? Context
        if (ctx != null) {
            appContextRef.set(ctx.applicationContext)
        }
    }

    actual fun getString(key: String, language: AppLanguage, args: List<String>): String {
        val ctx = appContextRef.get() ?: return key
        val resId = stringIdCache.getOrPut(key) {
            ctx.resources.getIdentifier(key, "string", ctx.packageName)
        }
        if (resId == 0) return key

        val locale = when (language) {
            AppLanguage.CROATIAN -> Locale("hr")
            AppLanguage.GERMAN -> Locale.GERMAN
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }

        val config = Configuration(ctx.resources.configuration)
        config.setLocale(locale)
        val localizedContext = ctx.createConfigurationContext(config)
        val raw = localizedContext.resources.getString(resId)
        if (args.isEmpty()) return raw
        return runCatching {
            String.format(locale, raw, *args.toTypedArray())
        }.getOrElse { e ->
            // Keep UI alive when translation contains malformed format tokens.
            if (e is IllegalFormatException) {
                raw
            } else {
                key
            }
        }
    }
}
