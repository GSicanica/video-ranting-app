package com.youtube.rating.shared.l10n

object L10n {
    private var language: AppLanguage = AppLanguage.ENGLISH

    fun init(platformContext: Any? = null) {
        PlatformStrings.init(platformContext)
    }

    fun setLanguage(language: AppLanguage) {
        this.language = language
    }

    fun getLanguage(): AppLanguage = language

    fun t(key: String): String = PlatformStrings.getString(key, language, emptyList())

    fun t(key: String, vararg args: Any?): String {
        if (args.isEmpty()) return t(key = key)
        val stringArgs = args.map { it?.toString() ?: "" }
        return PlatformStrings.getString(key, language, stringArgs)
    }

    fun t(key: String, args: List<String>): String =
        PlatformStrings.getString(key, language, args)
}
