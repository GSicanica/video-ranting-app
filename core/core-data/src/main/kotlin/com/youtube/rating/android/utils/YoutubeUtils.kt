package com.youtube.rating.android.utils


private val ID_REGEX = Regex("""[a-zA-Z0-9_-]{11}""")

fun extractVideoId(input: String): String? {
    val s = input.trim()
    if (s.isBlank()) return null

    // Direct ID
    if (ID_REGEX.matches(s)) return s

    // Common patterns (watch, youtu.be, embed, shorts) with extra params allowed
    val patterns = listOf(
        Regex("""(?:youtube\.com\/watch\?.*?[&?]v=)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:youtu\.be\/)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:youtube\.com\/shorts\/)([a-zA-Z0-9_-]{11})""")
    )

    for (p in patterns) {
        p.find(s)?.groupValues?.getOrNull(1)?.let { return it }
    }

    // Last-resort: find any 11-char id in string
    return ID_REGEX.find(s)?.value
}

fun getLanguageName(code: String): String {
    return when (code.lowercase()) {
        "hr" -> "Hrvatski"
        "en" -> "English"
        "de" -> "Deutsch"
        "es" -> "Español"
        "fr" -> "Français"
        "it" -> "Italiano"
        "pt" -> "Português"
        "ru" -> "Русский"
        "pl" -> "Polski"
        "uk" -> "Українська"
        "sr" -> "Српски"
        "bs" -> "Bosanski"
        "sl" -> "Slovenščina"
        "mk" -> "Македонски"
        "bg" -> "Български"
        "ro" -> "Română"
        "hu" -> "Magyar"
        "cs" -> "Čeština"
        "sk" -> "Slovenčina"
        else -> code.uppercase()
    }



}
