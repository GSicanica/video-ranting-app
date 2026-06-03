package com.youtube.rating.android.utils

import androidx.core.text.HtmlCompat
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object TextDecoding {
    private val percentEncodedRegex = Regex("%[0-9a-fA-F]{2}")

    fun decodePossiblyEncodedText(raw: String): String {
        if (raw.isEmpty()) return raw

        var s = raw

        // 1) Handle URL-encoded UTF-8 that might have been stored as plain text in DB.
        if (percentEncodedRegex.containsMatchIn(s)) {
            s = runCatching { URLDecoder.decode(s, StandardCharsets.UTF_8.name()) }.getOrDefault(s)
        }

        // 2) Handle HTML entities (&scaron;, &#269;, ...).
        if (s.indexOf('&') >= 0) {
            s = runCatching { HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
                .getOrDefault(s)
        }

        // 3) Handle mojibake: UTF-8 bytes interpreted as ISO-8859-1 (common in PHP/DB setups).
        // Example: "Š" becomes "Å " or "š" becomes "Å¡".
        s = fixMojibakeIso88591ToUtf8IfBetter(input = s)

        return s
    }

    private fun fixMojibakeIso88591ToUtf8IfBetter(input: String): String {
        // Fast bailouts for normal text.
        if (!looksLikeMojibake(s = input)) return input

        val converted = runCatching {
            String(input.toByteArray(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)
        }.getOrNull() ?: return input

        // Choose the version that "looks" more like Croatian (more diacritics),
        // and avoid making things worse.
        return if (croatianScore(s = converted) > croatianScore(s = input) && !converted.contains('\uFFFD')) {
            converted
        } else {
            input
        }
    }

    private fun looksLikeMojibake(s: String): Boolean {
        // Common mojibake marker characters in UTF-8->latin1 mistakes.
        return s.any { it == 'Ã' || it == 'Â' || it == 'Å' || it == 'Ä' || it == 'Ð' || it == 'â' }
    }

    private fun croatianScore(s: String): Int {
        var score = 0
        for (c in s) {
            when (c) {
                'č', 'ć', 'đ', 'š', 'ž', 'Č', 'Ć', 'Đ', 'Š', 'Ž' -> score++
            }
        }
        return score
    }
}

