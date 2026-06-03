package com.youtube.rating.shared.l10n

import platform.Foundation.NSBundle

actual object PlatformStrings {
    actual fun init(platformContext: Any?) {
        // no-op on iOS
    }

    actual fun getString(key: String, language: AppLanguage, args: List<String>): String {
        val langCode = when (language) {
            AppLanguage.CROATIAN -> "hr"
            AppLanguage.GERMAN -> "de"
            AppLanguage.ENGLISH -> "en"
        }
        val bundle = bundleForLanguage(code = langCode)
        var value = bundle.localizedStringForKey(key, key, null)
        if (args.isEmpty()) return value

        args.forEachIndexed { index, arg ->
            val token = "%${index + 1}$@"
            value = value.replace(token, arg)
        }
        return value
    }

    private fun bundleForLanguage(code: String): NSBundle {
        val path = NSBundle.mainBundle.pathForResource(code, "lproj")
        return if (path != null) NSBundle.bundleWithPath(path) ?: NSBundle.mainBundle
        else NSBundle.mainBundle
    }
}
