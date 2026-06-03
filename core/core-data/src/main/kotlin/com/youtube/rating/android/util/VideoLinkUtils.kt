package com.youtube.rating.android.util

import com.youtube.rating.core.coroutines.ioDispatcher


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.youtube.rating.android.sentry.SentryNetworkInterceptor
import com.youtube.rating.shared.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI

private val YT_ID_REGEX = Regex("""[a-zA-Z0-9_-]{11}""")
private val FB_NUM_ID_REGEX = Regex("""\b(\d{8,20})\b""")

private val httpClient by lazy {
    val allowedHosts = setOfNotNull(parseHost(url = BASE_URL))
    OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor(SentryNetworkInterceptor(allowedHosts = allowedHosts))
        .build()
}

/** Existing helper (kept), slightly extended */
fun extractYouTubeVideoId(input: String): String? {
    val s = input.trim()
    if (s.isBlank()) return null

    // Direct ID
    if (YT_ID_REGEX.matches(s)) return s

    val patterns = listOf(
        Regex("""(?:youtube\.com\/watch\?.*?[&?]v=)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:youtu\.be\/)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:youtube\.com\/shorts\/)([a-zA-Z0-9_-]{11})""")
    )

    for (p in patterns) {
        p.find(s)?.groupValues?.getOrNull(1)?.let { return it }
    }

    return YT_ID_REGEX.find(s)?.value
}

fun extractFacebookVideoId(input: String): String? {
    val s = input.trim()
    if (s.isBlank()) return null

    val patterns = listOf(
        Regex("""(?:facebook\.com\/watch\/?\?.*?[&?]v=)(\d{8,20})"""),
        Regex("""(?:facebook\.com\/reel\/)(\d{8,20})"""),
        Regex("""(?:facebook\.com\/.*\/videos\/)(\d{8,20})"""),
        Regex("""(?:facebook\.com\/video\.php\?.*?[&?]v=)(\d{8,20})"""),
        Regex("""(?:m\.facebook\.com\/watch\/?\?.*?[&?]v=)(\d{8,20})"""),
        Regex("""(?:m\.facebook\.com\/reel\/)(\d{8,20})""")
    )

    for (p in patterns) {
        p.find(s)?.groupValues?.getOrNull(1)?.let { return it }
    }

    return FB_NUM_ID_REGEX.find(s)?.groupValues?.getOrNull(1)
}

/**
 * Resolve fb.watch short links to final destination URL (follows redirects).
 * Returns original url if it is not fb.watch or if resolve fails.
 */
suspend fun resolveFacebookWatchUrl(url: String): String = withContext(ioDispatcher) {
    val trimmed = url.trim()
    if (!trimmed.contains("fb.watch/")) return@withContext trimmed

    try {
        val req = Request.Builder()
            .url(trimmed)
            .get()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("DNT", "1")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Cache-Control", "max-age=0")
            .build()

        httpClient.newCall(req).execute().use { resp ->
            resp.request.url.toString()
        }
    } catch (e: IOException) {
        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        trimmed
    } catch (e: IllegalArgumentException) {
        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        trimmed
    }
}

private fun looksLikeFacebookUrl(url: String): Boolean {
    val s = url.lowercase()
    return s.contains("facebook.com") || s.contains("m.facebook.com") || s.contains("fb.watch")
}

private fun normalizeUrl(input: String): String = input.trim()

private fun parseHost(url: String): String? {
    return try {
        URI(url).host?.lowercase()
    } catch (e: Exception) {
        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        null
    }
}

/**
 * Main function: detect YouTube vs Facebook (including fb.watch).
 * - YouTube: returns YouTube(videoId = videoId)
 * - Facebook: resolves fb.watch -> finalUrl, returns Facebook(resolvedUrl, optionalId, originalUrl)
 */
suspend fun detectVideoSource(input: String): VideoSource {
    val s = normalizeUrl(input = input)
    if (s.isBlank()) return VideoSource.Unknown("empty")

    extractYouTubeVideoId(input = s)?.let { return VideoSource.YouTube(it) }

    if (looksLikeFacebookUrl(url = s)) {
        val resolved = resolveFacebookWatchUrl(url = s)
        val fbId = extractFacebookVideoId(input = resolved)
        return VideoSource.Facebook(resolvedUrl = resolved, videoId = fbId, originalUrl = s)
    }

    return VideoSource.Unknown("not supported")
}

fun openExternalUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(intent)
}

data class UnifiedVideoInfo(
    val title: String,
    val channelName: String = "",
    val language: String = "unknown",
    val thumbnail: String = "",
    val siteName: String,   // npr. "Facebook"
    val imageUrl: String,
    val platform: String,
    val externalId: String?,
    val canonicalUrl: String
)


private val ogClient by lazy { OkHttpClient() }

private fun extractOg(html: String, property: String): String? {
    val r = Regex(
        """property=["']$property["']\s+content=["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )
    return r.find(html)?.groupValues?.getOrNull(1)
}

/**
 * Učita og:title i og:image sa Facebook stranice (ako je dostupno bez login-a).
 * Pokušava više puta sa različitim headerima ako prvi pokušaj ne uspije.
 */
suspend fun loadFacebookOpenGraphInfo(resolvedUrl: String): UnifiedVideoInfo =
    withContext(ioDispatcher) {

        val userAgents = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
        )

        var lastHtml = ""
        var lastException: Exception? = null

        // Try different user agents
        for (userAgent in userAgents) {
            try {
                val req = Request.Builder()
                    .url(resolvedUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("DNT", "1")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Cache-Control", "max-age=0")
                    .header("Referer", "https://www.facebook.com/")
                    .get()
                    .build()

                ogClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val html = resp.body?.string().orEmpty()
                        lastHtml = html

                        val title = extractOg(html = html, property = "og:title") ?: extractTitleFallback(html = html)
                        val image = extractOg(html = html, property = "og:image") ?: ""
                        val site = extractOg(html = html, property = "og:site_name") ?: "Facebook"

                        // If we got at least a title, return it
                        if (title.isNotBlank() && title != "Facebook video") {
                            return@withContext UnifiedVideoInfo(
                                title = title,
                                channelName = "Facebook",
                                thumbnail = image,
                                platform = "facebook",
                                externalId = extractFacebookVideoId(input = resolvedUrl),
                                canonicalUrl = resolvedUrl,
                                siteName = site,
                                imageUrl = image,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                lastException = e
            }
        }

        // If all attempts failed, try fallback extraction from HTML
        val fallbackTitle = extractTitleFallback(html = lastHtml)
        val fallbackImage = extractImageFallback(html = lastHtml)

        UnifiedVideoInfo(
            title = if (fallbackTitle.isNotBlank()) fallbackTitle else "Facebook video",
            channelName = "Facebook",
            thumbnail = fallbackImage,
            platform = "facebook",
            externalId = extractFacebookVideoId(input = resolvedUrl),
            canonicalUrl = resolvedUrl,
            siteName = "Facebook",
            imageUrl = fallbackImage,
        )
    }

/**
 * Fallback title extraction from HTML title tag
 */
private fun extractTitleFallback(html: String): String {
    val titleRegex = Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE)
    return titleRegex.find(html)?.groupValues?.getOrNull(1)?.trim() ?: ""
}

/**
 * Fallback image extraction from HTML
 */
private fun extractImageFallback(html: String): String {
    // Try to find Facebook video thumbnail patterns
    val patterns = listOf(
        Regex("https://[^\"']*\\.fbcdn\\.net/[^\"']*\\.(jpg|jpeg|png|webp)[^\"']*", RegexOption.IGNORE_CASE),
        Regex("https://external[^\"']*\\.fbcdn\\.net/[^\"']*\\.(jpg|jpeg|png|webp)[^\"']*", RegexOption.IGNORE_CASE),
        Regex("https://scontent[^\"']*\\.fbcdn\\.net/[^\"']*\\.(jpg|jpeg|png|webp)[^\"']*", RegexOption.IGNORE_CASE)
    )

    for (pattern in patterns) {
        pattern.find(html)?.let { match ->
            return match.value
        }
    }

    return ""
}


fun openFacebookUrl(context: Context, url: String) {
    val uri = url.toUri()

    // 1) pokušaj FB app
    val fbIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        setPackage("com.facebook.katana")
    }

    try {
        context.startActivity(fbIntent)
        return
    } catch (e: ActivityNotFoundException) {
        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        // FB app nije instaliran -> fallback
    }

    // 2) fallback browser (bez package)
    val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    context.startActivity(browserIntent)
}
