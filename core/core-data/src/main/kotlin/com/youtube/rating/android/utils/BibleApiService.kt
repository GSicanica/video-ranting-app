package com.youtube.rating.android.utils

import com.youtube.rating.core.coroutines.ioDispatcher

import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service za dohvaćanje citata iz Biblije putem API-ja
 * Koristi besplatni bible-api.com
 * Podržava više jezika i prijevoda
 */
object BibleApiService {
    private const val TAG = "BibleApiService"
    private const val YOUVERSION_BIBLE_ID_EN = "206" // KJV (YouVersion default)
    
    data class BibleApiResponse(
        val reference: String,
        val text: String,
        val translationId: String,
        val translationName: String
    )
    
    /**
     * Dostupni jezici i prijevodi Biblije
     * Napomena: bible-api.com nema prave hrvatske/njemačke prijevode
     * Prikazujemo engleski tekst ali s hrvatskim nazivima knjiga
     */
    enum class BibleLanguage(
        val code: String,
        val displayName: String,
        val translationId: String,
        val translationName: String,
        val bibleId: String
    ) {
        CROATIAN("hr", "Hrvatski 🇭🇷", "web", "World English Bible", YOUVERSION_BIBLE_ID_EN),
        ENGLISH("en", "English 🇬🇧", "web", "World English Bible", YOUVERSION_BIBLE_ID_EN),
        GERMAN("de", "Deutsch 🇩🇪", "web", "World English Bible", YOUVERSION_BIBLE_ID_EN);
        
        companion object {
            fun fromCode(code: String): BibleLanguage = 
                values().find { it.code == code } ?: CROATIAN
        }
    }
    
    // Popularne knjige Novog Zavjeta za random citate
    private val popularBooks = listOf(
        "JHN",    // Ivan
        "MAT",    // Matej
        "ROM",    // Rimljanima
        "1CO",    // 1 Korinćanima
        "PSA",    // Psalmi
        "PRO",    // Izreke
        "PHP",    // Filipljanima
        "GAL",    // Galaćanima
        "EPH",    // Efežanima
        "HEB",    // Hebrejima
        "JAS",    // Jakov
        "1JN",    // 1 Ivanova
        "1PE",    // 1 Petrova
        "ISA"     // Izaija
    )

    // Chapter counts for popular books (USFM codes)
    private val popularBookChapters = mapOf(
        "JHN" to 21,
        "MAT" to 28,
        "ROM" to 16,
        "1CO" to 16,
        "PSA" to 150,
        "PRO" to 31,
        "PHP" to 4,
        "GAL" to 6,
        "EPH" to 6,
        "HEB" to 13,
        "JAS" to 5,
        "1JN" to 5,
        "1PE" to 5,
        "ISA" to 66
    )

    private val bookIdToUsfm = mapOf(
        "postanak" to "GEN",
        "izlazak" to "EXO",
        "levitski-zakonik" to "LEV",
        "brojevi" to "NUM",
        "ponovljeni-zakon" to "DEU",
        "josua" to "JOS",
        "suci" to "JDG",
        "ruta" to "RUT",
        "1-samuelova" to "1SA",
        "2-samuelova" to "2SA",
        "1-kraljeva" to "1KI",
        "2-kraljeva" to "2KI",
        "1-ljetopisa" to "1CH",
        "2-ljetopisa" to "2CH",
        "ezra" to "EZR",
        "nehemija" to "NEH",
        "estera" to "EST",
        "job" to "JOB",
        "psalmi" to "PSA",
        "mudre-izreke" to "PRO",
        "propovjednik" to "ECC",
        "pjesma-nad-pjesmama" to "SNG",
        "izaija" to "ISA",
        "jeremija" to "JER",
        "tuzaljke" to "LAM",
        "ezekiel" to "EZK",
        "daniel" to "DAN",
        "hosea" to "HOS",
        "joel" to "JOL",
        "amos" to "AMO",
        "obadija" to "OBA",
        "jona" to "JON",
        "mihej" to "MIC",
        "nahum" to "NAM",
        "habakuk" to "HAB",
        "sefanija" to "ZEP",
        "hagaj" to "HAG",
        "zaharija" to "ZEC",
        "malahija" to "MAL",
        "matej" to "MAT",
        "marko" to "MRK",
        "luka" to "LUK",
        "ivan" to "JHN",
        "djela" to "ACT",
        "rimljanima" to "ROM",
        "1-korincanima" to "1CO",
        "2-korincanima" to "2CO",
        "galacanima" to "GAL",
        "efezanima" to "EPH",
        "filipljanima" to "PHP",
        "kolosanima" to "COL",
        "1-solunjanima" to "1TH",
        "2-solunjanima" to "2TH",
        "1-timoteju" to "1TI",
        "2-timoteju" to "2TI",
        "titu" to "TIT",
        "filemonu" to "PHM",
        "hebrejima" to "HEB",
        "jakovljeva" to "JAS",
        "1-petrova" to "1PE",
        "2-petrova" to "2PE",
        "1-ivanova" to "1JN",
        "2-ivanova" to "2JN",
        "3-ivanova" to "3JN",
        "judina" to "JUD",
        "otkrivenje" to "REV"
    )

    // English book name variants -> USFM (lowercase keys)
    private val englishBookNameToUsfm = mapOf(
        "genesis" to "GEN",
        "exodus" to "EXO",
        "leviticus" to "LEV",
        "numbers" to "NUM",
        "deuteronomy" to "DEU",
        "joshua" to "JOS",
        "judges" to "JDG",
        "ruth" to "RUT",
        "1 samuel" to "1SA",
        "2 samuel" to "2SA",
        "1 kings" to "1KI",
        "2 kings" to "2KI",
        "1 chronicles" to "1CH",
        "2 chronicles" to "2CH",
        "ezra" to "EZR",
        "nehemiah" to "NEH",
        "esther" to "EST",
        "job" to "JOB",
        "psalms" to "PSA",
        "psalm" to "PSA",
        "proverbs" to "PRO",
        "ecclesiastes" to "ECC",
        "song of solomon" to "SNG",
        "song of songs" to "SNG",
        "isaiah" to "ISA",
        "jeremiah" to "JER",
        "lamentations" to "LAM",
        "ezekiel" to "EZK",
        "daniel" to "DAN",
        "hosea" to "HOS",
        "joel" to "JOL",
        "amos" to "AMO",
        "obadiah" to "OBA",
        "jonah" to "JON",
        "micah" to "MIC",
        "nahum" to "NAM",
        "habakkuk" to "HAB",
        "zephaniah" to "ZEP",
        "haggai" to "HAG",
        "zechariah" to "ZEC",
        "malachi" to "MAL",
        "matthew" to "MAT",
        "mark" to "MRK",
        "luke" to "LUK",
        "john" to "JHN",
        "acts" to "ACT",
        "romans" to "ROM",
        "1 corinthians" to "1CO",
        "2 corinthians" to "2CO",
        "galatians" to "GAL",
        "ephesians" to "EPH",
        "philippians" to "PHP",
        "colossians" to "COL",
        "1 thessalonians" to "1TH",
        "2 thessalonians" to "2TH",
        "1 timothy" to "1TI",
        "2 timothy" to "2TI",
        "titus" to "TIT",
        "philemon" to "PHM",
        "hebrews" to "HEB",
        "james" to "JAS",
        "1 peter" to "1PE",
        "2 peter" to "2PE",
        "1 john" to "1JN",
        "2 john" to "2JN",
        "3 john" to "3JN",
        "jude" to "JUD",
        "revelation" to "REV"
    )
    
    /**
     * Dohvati nasumični stih iz Biblije
     * @param language Jezik prijevoda (default: Croatian)
     * @param bookIds Lista knjiga za random odabir (default: Novi Zavjet)
     */
    suspend fun getRandomVerse(
        language: BibleLanguage = BibleLanguage.ENGLISH,
        bookIds: String = "NT"
    ): Result<BibleApiResponse> {
        return withContext(ioDispatcher) {
            try {
                val book = popularBooks.random()
                val maxChapters = popularBookChapters[book] ?: 1
                val chapter = (1..maxChapters).random()
                val verse = (1..30).random()
                val passageId = "$book.$chapter.$verse"

                val response = fetchYouVersionPassage(
                    bibleId = language.bibleId,
                    passageId = passageId,
                )
                if (response.isFailure) {
                    return@withContext Result.failure(
                        response.exceptionOrNull() ?: IOException("Failed to fetch passage")
                    )
                }
                val bookName = translateBookCode(bookCode = book, language = language)
                val reference = if (bookName.isNotEmpty()) "$bookName $chapter:$verse" else "$book $chapter:$verse"
                Result.success(
                    BibleApiResponse(
                        reference = reference,
                        text = response.getOrNull().orEmpty(),
                        translationId = language.translationId,
                        translationName = language.translationName
                    )
                )
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "YouVersion API exception", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Dohvati specifični stih
     * @param reference npr. "john 3:16" ili "psalms 23:1"
     * @param language Jezik prijevoda
     */
    suspend fun getVerse(
        reference: String,
        language: BibleLanguage = BibleLanguage.ENGLISH
    ): Result<BibleApiResponse> {
        return withContext(ioDispatcher) {
            try {
                val passageId = normalizeReferenceToUsfm(reference = reference)

                val text = fetchYouVersionPassage(
                    bibleId = language.bibleId,
                    passageId = passageId,
                )
                if (text.isFailure) {
                    return@withContext Result.failure(
                        text.exceptionOrNull() ?: IOException("Failed to fetch passage")
                    )
                }

                val parsed = BibleApiResponse(
                    reference = reference,
                    text = text.getOrNull().orEmpty(),
                    translationId = language.translationId,
                    translationName = language.translationName
                )

                Logger.info(TAG, "Got verse: ${parsed.reference}")
                Result.success(parsed)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "YouVersion API exception", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Dohvati "Verse of the Day" - konzistentan stih za današnji dan
     * @param language Jezik prijevoda
     */
    suspend fun getVerseOfTheDay(language: BibleLanguage = BibleLanguage.ENGLISH): Result<BibleApiResponse> {
        // Koristi dan u godini za konzistentan odabir
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val bookIndex = dayOfYear % popularBooks.size
        val book = popularBooks[bookIndex]
        
        return getRandomVerse(language = language, bookIds = book)
    }
    
    /**
     * Dohvati inspirativni stih iz popularne knjige
     * @param language Jezik prijevoda
     */
    suspend fun getInspirationalVerse(language: BibleLanguage = BibleLanguage.ENGLISH): Result<BibleApiResponse> {
        val randomBook = popularBooks.random()
        return getRandomVerse(language = language, bookIds = randomBook)
    }

    private fun fetchYouVersionPassage(
        bibleId: String,
        passageId: String,
    ): Result<String> {
        val encodedPassage = java.net.URLEncoder.encode(passageId, "UTF-8")
        val encodedBibleId = java.net.URLEncoder.encode(bibleId, "UTF-8")
        val url = URL(
            "${com.youtube.rating.shared.BASE_URL}/api/bible/youversion/passage.php" +
                "?bibleId=$encodedBibleId&passageId=$encodedPassage"
        )
        val connection = (url.openConnection() as? HttpURLConnection)
        if (connection == null) {
            val e = IOException("Failed to create HTTP connection")
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            return Result.failure(e)
        }
        connection.requestMethod = "GET"
        connection.connectTimeout = 7000
        connection.readTimeout = 7000
        connection.setRequestProperty("Accept", "application/json")

        return try {
            if (connection.responseCode != 200) {
                Result.failure(IOException("Bible proxy error: ${connection.responseCode}"))
            } else {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (!json.optBoolean("success", false)) {
                    Result.failure(IOException(json.optString("message", "Bible proxy error")))
                } else {
                    val data = json.optJSONObject("data")
                    val content = data?.optString("content", "") ?: json.optString("content", "")
                    Result.success(content.replace(Regex("<[^>]*>"), "").trim())
                }
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Result.failure(e)
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    suspend fun getPassage(
        bookId: String,
        chapter: Int,
        language: BibleLanguage = BibleLanguage.ENGLISH
    ): Result<BibleApiResponse> {
        return withContext(ioDispatcher) {
            try {
                val usfm = resolveBookToUsfm(bookId = bookId) ?: return@withContext Result.failure(
                    IllegalArgumentException("Unknown book id: $bookId")
                )
                val passageId = "$usfm.$chapter"
                val textRes = fetchYouVersionPassage(
                    bibleId = language.bibleId,
                    passageId = passageId,
                )
                if (textRes.isFailure) return@withContext Result.failure(textRes.exceptionOrNull() ?: IOException("Failed to fetch passage"))
                val text = textRes.getOrNull().orEmpty()
                val bookName = translateBookCode(bookCode = usfm, language = language)
                val reference = if (bookName.isNotEmpty()) "$bookName $chapter" else "$usfm $chapter"
                Result.success(
                    BibleApiResponse(
                        reference = reference,
                        text = text,
                        translationId = language.translationId,
                        translationName = language.translationName
                    )
                )
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "YouVersion passage exception", e)
                Result.failure(e)
            }
        }
    }
    
    // Mapiranje book kodova na nazive po jezicima
    private val bookCodeToCroatian = mapOf(
        "GEN" to "Postanak", "EXO" to "Izlazak", "LEV" to "Levitski zakonik",
        "NUM" to "Brojevi", "DEU" to "Ponovljeni zakon", "JOS" to "Jošua",
        "JDG" to "Suci", "RUT" to "Ruta", "1SA" to "1 Samuelova", "2SA" to "2 Samuelova",
        "1KI" to "1 Kraljevima", "2KI" to "2 Kraljevima", "1CH" to "1 Ljetopisa",
        "2CH" to "2 Ljetopisa", "EZR" to "Ezra", "NEH" to "Nehemija", "EST" to "Estera",
        "JOB" to "Job", "PSA" to "Psalmi", "PRO" to "Izreke", "ECC" to "Propovjednik",
        "SNG" to "Pjesma nad pjesmama", "ISA" to "Izaija", "JER" to "Jeremija",
        "LAM" to "Tužaljke", "EZK" to "Ezekiel", "DAN" to "Daniel", "HOS" to "Hošea",
        "JOL" to "Joel", "AMO" to "Amos", "OBA" to "Obadija", "JON" to "Jona",
        "MIC" to "Mihej", "NAM" to "Nahum", "HAB" to "Habakuk", "ZEP" to "Sefanija",
        "HAG" to "Hagaj", "ZEC" to "Zaharija", "MAL" to "Malahija",
        "MAT" to "Matej", "MRK" to "Marko", "LUK" to "Luka", "JHN" to "Ivan",
        "ACT" to "Djela apostolska", "ROM" to "Rimljanima",
        "1CO" to "1 Korinćanima", "2CO" to "2 Korinćanima",
        "GAL" to "Galaćanima", "EPH" to "Efežanima", "PHP" to "Filipljanima",
        "COL" to "Kološanima", "1TH" to "1 Solunjanima", "2TH" to "2 Solunjanima",
        "1TI" to "1 Timoteju", "2TI" to "2 Timoteju", "TIT" to "Titu",
        "PHM" to "Filemonu", "HEB" to "Hebrejima", "JAS" to "Jakov",
        "1PE" to "1 Petrova", "2PE" to "2 Petrova",
        "1JN" to "1 Ivanova", "2JN" to "2 Ivanova", "3JN" to "3 Ivanova",
        "JUD" to "Juda", "REV" to "Otkrivenje"
    )
    
    private val bookCodeToGerman = mapOf(
        "GEN" to "Genesis", "EXO" to "Exodus", "LEV" to "Levitikus",
        "NUM" to "Numeri", "DEU" to "Deuteronomium", "JOS" to "Josua",
        "JDG" to "Richter", "RUT" to "Rut", "1SA" to "1 Samuel", "2SA" to "2 Samuel",
        "1KI" to "1 Könige", "2KI" to "2 Könige", "1CH" to "1 Chronik",
        "2CH" to "2 Chronik", "EZR" to "Esra", "NEH" to "Nehemia", "EST" to "Ester",
        "JOB" to "Hiob", "PSA" to "Psalmen", "PRO" to "Sprüche", "ECC" to "Prediger",
        "SNG" to "Hohelied", "ISA" to "Jesaja", "JER" to "Jeremia",
        "LAM" to "Klagelieder", "EZK" to "Hesekiel", "DAN" to "Daniel", "HOS" to "Hosea",
        "JOL" to "Joel", "AMO" to "Amos", "OBA" to "Obadja", "JON" to "Jona",
        "MIC" to "Micha", "NAM" to "Nahum", "HAB" to "Habakuk", "ZEP" to "Zefanja",
        "HAG" to "Haggai", "ZEC" to "Sacharja", "MAL" to "Maleachi",
        "MAT" to "Matthäus", "MRK" to "Markus", "LUK" to "Lukas", "JHN" to "Johannes",
        "ACT" to "Apostelgeschichte", "ROM" to "Römer",
        "1CO" to "1 Korinther", "2CO" to "2 Korinther",
        "GAL" to "Galater", "EPH" to "Epheser", "PHP" to "Philipper",
        "COL" to "Kolosser", "1TH" to "1 Thessalonicher", "2TH" to "2 Thessalonicher",
        "1TI" to "1 Timotheus", "2TI" to "2 Timotheus", "TIT" to "Titus",
        "PHM" to "Philemon", "HEB" to "Hebräer", "JAS" to "Jakobus",
        "1PE" to "1 Petrus", "2PE" to "2 Petrus",
        "1JN" to "1 Johannes", "2JN" to "2 Johannes", "3JN" to "3 Johannes",
        "JUD" to "Judas", "REV" to "Offenbarung"
    )
    
    private val bookCodeToEnglish = mapOf(
        "GEN" to "Genesis", "EXO" to "Exodus", "LEV" to "Leviticus",
        "NUM" to "Numbers", "DEU" to "Deuteronomy", "JOS" to "Joshua",
        "JDG" to "Judges", "RUT" to "Ruth", "1SA" to "1 Samuel", "2SA" to "2 Samuel",
        "1KI" to "1 Kings", "2KI" to "2 Kings", "1CH" to "1 Chronicles",
        "2CH" to "2 Chronicles", "EZR" to "Ezra", "NEH" to "Nehemiah", "EST" to "Esther",
        "JOB" to "Job", "PSA" to "Psalms", "PRO" to "Proverbs", "ECC" to "Ecclesiastes",
        "SNG" to "Song of Solomon", "ISA" to "Isaiah", "JER" to "Jeremiah",
        "LAM" to "Lamentations", "EZK" to "Ezekiel", "DAN" to "Daniel", "HOS" to "Hosea",
        "JOL" to "Joel", "AMO" to "Amos", "OBA" to "Obadiah", "JON" to "Jonah",
        "MIC" to "Micah", "NAM" to "Nahum", "HAB" to "Habakkuk", "ZEP" to "Zephaniah",
        "HAG" to "Haggai", "ZEC" to "Zechariah", "MAL" to "Malachi",
        "MAT" to "Matthew", "MRK" to "Mark", "LUK" to "Luke", "JHN" to "John",
        "ACT" to "Acts", "ROM" to "Romans",
        "1CO" to "1 Corinthians", "2CO" to "2 Corinthians",
        "GAL" to "Galatians", "EPH" to "Ephesians", "PHP" to "Philippians",
        "COL" to "Colossians", "1TH" to "1 Thessalonians", "2TH" to "2 Thessalonians",
        "1TI" to "1 Timothy", "2TI" to "2 Timothy", "TIT" to "Titus",
        "PHM" to "Philemon", "HEB" to "Hebrews", "JAS" to "James",
        "1PE" to "1 Peter", "2PE" to "2 Peter",
        "1JN" to "1 John", "2JN" to "2 John", "3JN" to "3 John",
        "JUD" to "Jude", "REV" to "Revelation"
    )
    
    /**
     * Prevedi book kod (npr. "JHN") u lokalizirani naziv knjige
     */
    private fun translateBookCode(bookCode: String, language: BibleLanguage): String {
        return when (language) {
            BibleLanguage.CROATIAN -> bookCodeToCroatian[bookCode] ?: bookCode
            BibleLanguage.GERMAN -> bookCodeToGerman[bookCode] ?: bookCode
            BibleLanguage.ENGLISH -> bookCodeToEnglish[bookCode] ?: bookCode
        }
    }

    private fun normalizeReferenceToUsfm(reference: String): String {
        val cleaned = reference.trim()
            .replace(Regex("\\s+"), " ")
            .replace(".", " ")
            .replace(":", " ")
        val match = Regex("^(.+?)\\s+(\\d+)(?:\\s+(\\d+))?$", RegexOption.IGNORE_CASE)
            .find(cleaned)
        if (match == null) {
            return reference
                .replace(" ", ".")
                .replace(":", ".")
                .trim()
        }
        val bookRaw = match.groupValues[1].trim()
        val chapter = match.groupValues[2]
        val verse = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }

        val usfm = resolveBookToUsfm(bookId = bookRaw)
            ?: return reference
                .replace(" ", ".")
                .replace(":", ".")
                .trim()

        return if (verse != null) "$usfm.$chapter.$verse" else "$usfm.$chapter"
    }

    private fun resolveBookToUsfm(bookId: String): String? {
        val key = bookId.trim().lowercase()
        return bookIdToUsfm[key] ?: englishBookNameToUsfm[key]
    }
    
    // Mapiranje engleskih naziva knjiga na hrvatske (za stare reference)
    private val bookNameTranslations = mapOf(
        "Genesis" to "Postanak",
        "Exodus" to "Izlazak", 
        "Leviticus" to "Levitski zakonik",
        "Numbers" to "Brojevi",
        "Deuteronomy" to "Ponovljeni zakon",
        "Joshua" to "Jošua",
        "Judges" to "Suci",
        "Ruth" to "Ruta",
        "1 Samuel" to "1 Samuelova",
        "2 Samuel" to "2 Samuelova",
        "1 Kings" to "1 Kraljevima",
        "2 Kings" to "2 Kraljevima",
        "1 Chronicles" to "1 Ljetopisa",
        "2 Chronicles" to "2 Ljetopisa",
        "Ezra" to "Ezra",
        "Nehemiah" to "Nehemija",
        "Esther" to "Estera",
        "Job" to "Job",
        "Psalms" to "Psalmi",
        "Psalm" to "Psalam",
        "Proverbs" to "Izreke",
        "Ecclesiastes" to "Propovjednik",
        "Song of Solomon" to "Pjesma nad pjesmama",
        "Isaiah" to "Izaija",
        "Jeremiah" to "Jeremija",
        "Lamentations" to "Tužaljke",
        "Ezekiel" to "Ezekiel",
        "Daniel" to "Daniel",
        "Hosea" to "Hošea",
        "Joel" to "Joel",
        "Amos" to "Amos",
        "Obadiah" to "Obadija",
        "Jonah" to "Jona",
        "Micah" to "Mihej",
        "Nahum" to "Nahum",
        "Habakkuk" to "Habakuk",
        "Zephaniah" to "Sefanija",
        "Haggai" to "Hagaj",
        "Zechariah" to "Zaharija",
        "Malachi" to "Malahija",
        "Matthew" to "Matej",
        "Mark" to "Marko",
        "Luke" to "Luka",
        "John" to "Ivan",
        "Acts" to "Djela apostolska",
        "Romans" to "Rimljanima",
        "1 Corinthians" to "1 Korinćanima",
        "2 Corinthians" to "2 Korinćanima",
        "Galatians" to "Galaćanima",
        "Ephesians" to "Efežanima",
        "Philippians" to "Filipljanima",
        "Colossians" to "Kološanima",
        "1 Thessalonians" to "1 Solunjanima",
        "2 Thessalonians" to "2 Solunjanima",
        "1 Timothy" to "1 Timoteju",
        "2 Timothy" to "2 Timoteju",
        "Titus" to "Titu",
        "Philemon" to "Filemonu",
        "Hebrews" to "Hebrejima",
        "James" to "Jakov",
        "1 Peter" to "1 Petrova",
        "2 Peter" to "2 Petrova",
        "1 John" to "1 Ivanova",
        "2 John" to "2 Ivanova",
        "3 John" to "3 Ivanova",
        "Jude" to "Juda",
        "Revelation" to "Otkrivenje"
    )
    
    /**
     * Prevedi referencu na hrvatski
     */
    fun translateReference(englishReference: String): String {
        var croatianRef = englishReference
        for ((english, croatian) in bookNameTranslations) {
            croatianRef = croatianRef.replace(english, croatian, ignoreCase = true)
        }
        return croatianRef
    }
}
