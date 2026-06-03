package com.youtube.rating.shared.l10n

expect object PlatformStrings {
    fun init(platformContext: Any? = null)
    fun getString(key: String, language: AppLanguage, args: List<String> = emptyList()): String
}
