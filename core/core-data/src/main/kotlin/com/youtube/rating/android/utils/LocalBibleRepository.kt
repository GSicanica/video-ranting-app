package com.youtube.rating.android.utils

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import com.youtube.rating.core.data.R
import com.youtube.rating.android.data.BibleBooks
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object LocalBibleRepository {
    private const val BASE_PATH = "output1"
    private val GERMAN_RAW_ID = R.raw.de_bible

    @Volatile
    private var cachedBooks: List<String>? = null

    private val cachedChapters = ConcurrentHashMap<String, List<String>>()
    private val chapterCache = object : LinkedHashMap<String, String>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 24
        }
    }
    @Volatile
    private var indexLoaded = false

    @Volatile
    private var germanLoaded = false
    private val germanChapterCache = ConcurrentHashMap<String, String>()
    private val germanBookChapterText = ConcurrentHashMap<String, Map<Int, String>>()

    private fun germanKeyForIndex(idx: Int): String = "idx:$idx"
    private val germanBookNameToIndex = ConcurrentHashMap<String, Int>()
    private val germanBookNameById = mapOf(
        "postanak" to "1.Mose",
        "izlazak" to "2.Mose",
        "levitski-zakonik" to "3.Mose",
        "brojevi" to "4.Mose",
        "ponovljeni-zakon" to "5.Mose",
        "josua" to "Josua",
        "suci" to "Richter",
        "ruta" to "Rut",
        "1-samuelova" to "1.Samuel",
        "2-samuelova" to "2.Samuel",
        "1-kraljeva" to "1.Könige",
        "2-kraljeva" to "2.Könige",
        "1-ljetopisa" to "1.Chronik",
        "2-ljetopisa" to "2.Chronik",
        "ezra" to "Esra",
        "nehemija" to "Nehemia",
        "estera" to "Ester",
        "job" to "Hiob",
        "psalmi" to "Psalmen",
        "mudre-izreke" to "Sprichwörter",
        "propovjednik" to "Kohelet",
        "pjesma-nad-pjesmama" to "Hoheslied",
        "izaija" to "Jesaja",
        "jeremija" to "Jeremia",
        "tuzaljke" to "Klagelieder",
        "ezekiel" to "Ezechiel",
        "daniel" to "Daniel",
        "hosea" to "Hosea",
        "joel" to "Joel",
        "amos" to "Amos",
        "obadija" to "Obadja",
        "jona" to "Jona",
        "mihej" to "Micha",
        "nahum" to "Nahum",
        "habakuk" to "Habakuk",
        "sefanija" to "Zefanja",
        "hagaj" to "Haggai",
        "zaharija" to "Sacharja",
        "malahija" to "Maleachi",
        "matej" to "Matthäus",
        "marko" to "Markus",
        "luka" to "Lukas",
        "ivan" to "Johannes",
        "djela" to "Apostelgeschichte",
        "rimljanima" to "Römer",
        "1-korincanima" to "1.Korinther",
        "2-korincanima" to "2.Korinther",
        "galacanima" to "Galater",
        "efezanima" to "Epheser",
        "filipljanima" to "Philipper",
        "kolosanima" to "Kolosser",
        "1-solunjanima" to "1.Thessalonicher",
        "2-solunjanima" to "2.Thessalonicher",
        "1-timoteju" to "1.Timotheus",
        "2-timoteju" to "2.Timotheus",
        "titu" to "Titus",
        "filemonu" to "Philemon",
        "hebrejima" to "Hebräer",
        "jakovljeva" to "Jakobus",
        "1-petrova" to "1.Petrus",
        "2-petrova" to "2.Petrus",
        "1-ivanova" to "1.Johannes",
        "2-ivanova" to "2.Johannes",
        "3-ivanova" to "3.Johannes",
        "judina" to "Judas",
        "otkrivenje" to "Offenbarung"
    )

    suspend fun getBooks(context: Context): List<String> {
        cachedBooks?.let { return it }
        return withContext(ioDispatcher) {
            loadIndexIfNeeded(context = context)
            val books = cachedBooks ?: listAssets(context = context, path = BASE_PATH)
            cachedBooks = books
            books
        }
    }

    suspend fun getChapters(context: Context, bookId: String): List<String> {
        val normalizedBook = normalizeBook(bookId = bookId)
        cachedChapters[normalizedBook]?.let { return it }
        return withContext(ioDispatcher) {
            loadIndexIfNeeded(context = context)
            val chapters = cachedChapters[normalizedBook]
                ?: listAssets(context = context, path = "$BASE_PATH/$normalizedBook")
                    .filter { it.endsWith(".md", ignoreCase = true) }
            cachedChapters[normalizedBook] = chapters
            chapters
        }
    }

    /**
     * Best-effort resolver that maps various book IDs (API slugs, human-ish IDs like "2-ljetopisa",
     * etc.) to actual on-device asset directory keys under assets/output1.
     *
     * This is needed because the app historically used simplified IDs (e.g. "2-ljetopisa") while
     * the bundled content uses directory names like "druga-knjiga-ljetopisa".
     */
    suspend fun resolveBookId(context: Context, bookId: String): String = withContext(ioDispatcher) {
        loadIndexIfNeeded(context = context)

        val books = cachedBooks ?: listAssets(context = context, path = BASE_PATH)
        if (books.isEmpty()) return@withContext normalizeBook(bookId = bookId)

        val input = normalizeBook(bookId = bookId)
        val inputNorm = normalizeKey(s = input)

        // Explicit aliases for common book IDs -> asset directory names
        val alias = mapOf(
            "matej" to "evandelje-po-mateju",
            "marko" to "evandelje-po-marku",
            "luka" to "evandelje-po-luki",
            "ivan" to "evandelje-po-ivanu",
            "djela" to "djela-apostolska",
            "postanak" to "knjiga-postanka",
            "izlazak" to "knjiga-izlaska",
            "brojevi" to "knjiga-brojeva",
            "ruta" to "knjiga-o-ruti",
            "suci" to "knjiga-o-sucima",
            "job" to "knjiga-o-jobu"
        )
        alias[inputNorm]?.let { target ->
            if (books.any { it == target }) return@withContext target
        }

        // Fast paths
        books.firstOrNull { it == input }?.let { return@withContext it }
        books.firstOrNull { normalizeKey(s = it) == inputNorm }?.let { return@withContext it }

        val variants = expandVariants(inputNorm = inputNorm)
        val variantStems = variants.map { stemKey(s = it) }.toSet()

        var best: String? = null
        var bestScore = Int.MIN_VALUE

        for (b in books) {
            val cand = normalizeKey(s = b)
            val candStem = stemKey(s = cand)
            var score = 0

            if (variants.contains(cand)) score += 2000
            if (cand == inputNorm) score += 1800
            if (candStem in variantStems) score += 1400

            for (v in variants) {
                if (v.isEmpty()) continue
                if (cand.contains(v)) score += 900
                if (v.contains(cand) && cand.length >= 5) score += 200
                score += tokenOverlapScore(a = cand, b = v)
                score += commonPrefixScore(a = candStem, b = stemKey(v))
            }

            // Small penalty to avoid very long candidates winning only by containing.
            score -= kotlin.math.abs(cand.length - inputNorm.length)

            if (score > bestScore) {
                bestScore = score
                best = b
            }
        }

        // Threshold: avoid returning garbage for unknown inputs.
        if (bestScore >= 900) best ?: normalizeBook(bookId = bookId) else normalizeBook(bookId = bookId)
    }

    /**
     * Like [readChapter], but returns null when not found and does not report to Sentry.
     * Useful for "try local first, then fallback to network" flows.
     */
    suspend fun readChapterOrNull(context: Context, bookId: String, chapterFile: String): String? {
        val normalizedBook = normalizeBook(bookId = bookId)
        val normalizedChapter = chapterFile.substringAfterLast('/')
        val key = "$normalizedBook/$normalizedChapter"
        synchronized(chapterCache) {
            chapterCache[key]?.let { return it }
        }
        return withContext(ioDispatcher) {
            for (path in buildChapterAssetCandidates(normalizedBook = normalizedBook, chapterFile = chapterFile, normalizedChapter = normalizedChapter)) {
                tryReadAssetTextOrNull(context = context, path = path)?.let { text ->
                    synchronized(chapterCache) { chapterCache[key] = text }
                    return@withContext text
                }
            }
            null
        }
    }

    /**
     * Load German bible chapter from raw JSON (bibel_de.json). Returns null when not found.
     */
    suspend fun readGermanChapterOrNull(context: Context, bookId: String, chapter: Int): String? {
        val key = "${bookId.lowercase()}/$chapter"
        germanChapterCache[key]?.let { return it }
        return withContext(ioDispatcher) {
            loadGermanIfNeeded(context = context)
            val bookKey = resolveGermanBookKey(bookId = bookId) ?: return@withContext null
            val chapterMap = germanBookChapterText[bookKey] ?: return@withContext null
            val text = chapterMap[chapter]?.trimEnd()
            if (!text.isNullOrBlank()) {
                germanChapterCache[key] = text
            }
            text
        }
    }

    suspend fun readChapter(context: Context, bookId: String, chapterFile: String): String {
        val normalizedBook = normalizeBook(bookId = bookId)
        val normalizedChapter = chapterFile.substringAfterLast('/')
        val key = "$normalizedBook/$normalizedChapter"
        synchronized(chapterCache) {
            chapterCache[key]?.let { return it }
        }
        return withContext(ioDispatcher) {
            var lastError: Exception? = null
            for (path in buildChapterAssetCandidates(normalizedBook = normalizedBook, chapterFile = chapterFile, normalizedChapter = normalizedChapter)) {
                try {
                    val text = requireNotNull(tryReadAssetTextOrNull(context = context, path = path))
                    synchronized(chapterCache) {
                        chapterCache[key] = text
                    }
                    return@withContext text
                } catch (e: Exception) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    lastError = e
                }
            }
            val e = lastError ?: IllegalStateException("Chapter not found: $bookId/$chapterFile")
            // Missing local asset should not be a high-severity error by itself; callers can fallback to network.
            // Still leave a breadcrumb for debugging.
            if (e !is java.io.FileNotFoundException) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(
                    e,
                    tags = mapOf(
                        "where" to "LocalBibleRepository.loadChapterText",
                        "book_id" to bookId,
                        "chapter_file" to chapterFile
                    )
                )
            } else {
                com.youtube.rating.android.sentry.SentryLogger.addBreadcrumb(
                    category = "bible.assets",
                    message = "Missing local chapter asset",
                    data = mapOf(
                        "book_id" to bookId,
                        "chapter_file" to chapterFile
                    )
                )
            }
            ""
        }
    }

    private fun buildChapterAssetCandidates(
        normalizedBook: String,
        chapterFile: String,
        normalizedChapter: String
    ): List<String> {
        val chapterNames = buildChapterNameTries(normalizedChapter = normalizedChapter)
        return chapterNames
            .flatMap { chapterName ->
                listOf(
                    "$BASE_PATH/$normalizedBook/$chapterName",
                    "$BASE_PATH/${chapterFile.removePrefix("$BASE_PATH/")}",
                    chapterFile.removePrefix("$BASE_PATH/")
                )
            }
            .distinct()
    }

    private fun buildChapterNameTries(normalizedChapter: String): List<String> {
        var ch = normalizedChapter
        if (!ch.endsWith(".md", ignoreCase = true)) ch += ".md"

        val label = ch.removeSuffix(".md")
        if (!label.all { it.isDigit() }) return listOf(ch)

        val n = label.toInt()
        return listOf(
            n.toString().padStart(3, '0') + ".md",
            n.toString().padStart(2, '0') + ".md",
            n.toString() + ".md"
        )
    }

    private fun tryReadAssetTextOrNull(context: Context, path: String): String? {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }

    private fun listAssets(context: Context, path: String): List<String> {
        return context.assets.list(path)?.filter { it.isNotBlank() && !it.startsWith(".") } ?: emptyList()
    }

    suspend fun preloadIndex(context: Context) = withContext(ioDispatcher) {
        loadIndexIfNeeded(context = context)
    }

    private fun normalizeBook(bookId: String): String =
        bookId.removePrefix("$BASE_PATH/").removePrefix("/").trim('/')

    private fun normalizeKey(s: String): String {
        val folded = buildString(s.length) {
            for (c in s.trim()) {
                append(
                    when (c) {
                        'č', 'ć', 'Č', 'Ć' -> 'c'
                        'đ', 'Đ' -> 'd'
                        'š', 'Š' -> 's'
                        'ž', 'Ž' -> 'z'
                        else -> c
                    }
                )
            }
        }
        return folded
            .lowercase()
            .replace('_', '-')
            .replace(' ', '-')
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    private fun stemKey(s: String): String {
        var x = normalizeKey(s = s)
        // remove common wrappers to make matching easier
        for (p in listOf("knjiga-", "poslanica-", "evandelje-po-", "psalmi-")) {
            if (x.startsWith(p)) x = x.removePrefix(p)
        }
        // remove connector token "o" (e.g. "knjiga-o-samuelu")
        x = x.replace("-o-", "-")
        // rough case-ending stripping (hr)
        x = x.replace(Regex("(ovima|evima|ima|ama|ovima)$"), "")
        x = x.replace(Regex("(ka|ke)$"), "")
        x = x.replace(Regex("(a|e|i|o|u)$"), "")
        x = x.replace(Regex("(ak|ek)$"), "")
        return x.trim('-')
    }

    private fun expandVariants(inputNorm: String): Set<String> {
        val out = linkedSetOf<String>()
        out += inputNorm

        val m = Regex("^([123])-(.+)$").find(inputNorm)
        if (m != null) {
            val n = m.groupValues[1]
            val rest = m.groupValues[2]
            val ord = when (n) {
                "1" -> "prva"
                "2" -> "druga"
                "3" -> "treca"
                else -> null
            }
            if (ord != null) {
                out += "$ord-$rest"
                out += "$ord-poslanica-$rest"
                out += "$ord-$rest-poslanica"
                out += "$ord-knjiga-$rest"
                out += "$ord-knjiga-o-$rest"
            }
        }

        return out
    }

    private fun tokenOverlapScore(a: String, b: String): Int {
        val at = a.split('-').filter { it.isNotBlank() }.toSet()
        val bt = b.split('-').filter { it.isNotBlank() }.toSet()
        if (at.isEmpty() || bt.isEmpty()) return 0
        val overlap = at.count { it in bt }
        return overlap * 140
    }

    private fun commonPrefixScore(a: String, b: String): Int {
        if (a.isEmpty() || b.isEmpty()) return 0
        val max = minOf(a.length, b.length)
        var n = 0
        while (n < max && a[n] == b[n]) n++
        return when {
            n >= 6 -> 650
            n >= 4 -> 350
            n >= 3 -> 180
            else -> 0
        }
    }

    private fun loadIndexIfNeeded(context: Context) {
        if (indexLoaded) return
        synchronized(this) {
            if (indexLoaded) return
            try {
                val json = context.assets.open("bible_index.json")
                    .bufferedReader()
                    .use { it.readText() }
                val obj = JSONObject(json)
                val books = mutableListOf<String>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    books.add(key)
                    val array = obj.optJSONArray(key)
                    if (array != null) {
                        val chapters = List(array.length()) { idx -> array.optString(idx) }
                        cachedChapters[key] = chapters
                    }
                }
                cachedBooks = books
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                // Fallback to asset listing if index missing or invalid.
            } finally {
                indexLoaded = true
            }
        }
    }

    private fun loadGermanIfNeeded(context: Context) {
        if (germanLoaded) return
        synchronized(this) {
            if (germanLoaded) return
            try {
                val json = context.resources.openRawResource(GERMAN_RAW_ID)
                    .bufferedReader()
                    .use { it.readText() }
                val obj = JSONObject(json)
                val books = obj.optJSONArray("books") ?: return
                for (i in 0 until books.length()) {
                    val bookObj = books.optJSONObject(i) ?: continue
                    val bookKey = germanKeyForIndex(idx = i)
                    val bookName = bookObj.optString("book").trim()
                    if (bookName.isNotBlank()) {
                        germanBookNameToIndex[normalizeGermanName(s = bookName)] = i
                    }
                    val verses = bookObj.optJSONArray("verses") ?: continue
                    val chapters = mutableMapOf<Int, StringBuilder>()
                    for (j in 0 until verses.length()) {
                        val v = verses.optJSONObject(j) ?: continue
                        val ch = v.optInt("chapter", -1)
                        val value = v.optString("text", "").trim()
                        if (ch <= 0 || value.isBlank()) continue
                        val sb = chapters.getOrPut(ch) { StringBuilder() }
                        sb.append(value).append('\n')
                    }
                    val out = chapters.mapValues { it.value.toString().trimEnd() }
                    germanBookChapterText[bookKey] = out
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            } finally {
                germanLoaded = true
            }
        }
    }

    private fun resolveGermanBookKey(bookId: String): String? {
        val name = germanBookNameById[bookId] ?: return null
        val idx = germanBookNameToIndex[normalizeGermanName(s = name)] ?: return null
        return germanKeyForIndex(idx = idx)
    }

    private fun normalizeGermanName(s: String): String {
        val folded = buildString(s.length) {
            for (c in s.trim()) {
                append(
                    when (c) {
                        'ä', 'Ä' -> 'a'
                        'ö', 'Ö' -> 'o'
                        'ü', 'Ü' -> 'u'
                        'ß' -> 's'
                        else -> c
                    }
                )
            }
        }
        return folded
            .lowercase()
            .replace(".", "")
            .replace(" ", "")
    }
}
