package com.youtube.rating.android.utils

/**
 * Generator citata iz Biblije koristeći API
 * Podržava više jezika i prijevoda
 */
object BibleQuoteGenerator {
    
    data class BibleQuote(
        val text: String,
        val reference: String,
        val language: BibleApiService.BibleLanguage
    )
    
    /**
     * Dohvati citat dana - konzistentan kroz cijeli dan
     */
    suspend fun getQuoteOfTheDay(
        language: BibleApiService.BibleLanguage = BibleApiService.BibleLanguage.CROATIAN
    ): BibleQuote {
        return try {
            if (language == BibleApiService.BibleLanguage.CROATIAN) {
                return getCroatianQuoteOfTheDay()
            }
            if (language == BibleApiService.BibleLanguage.GERMAN) {
                return getGermanQuoteOfTheDay()
            }
            val result = BibleApiService.getVerseOfTheDay(language)
            result.getOrNull()?.let {
                BibleQuote(
                    text = it.text,
                    reference = it.reference,
                    language = language
                )
            } ?: getDefaultQuote(language = language)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            println("youtubeRating: Error getting quote of the day: ${e.message}")
            getDefaultQuote(language = language)
        }
    }
    
    /**
     * Dohvati nasumični inspirativni citat
     */
    suspend fun getRandomQuote(
        language: BibleApiService.BibleLanguage = BibleApiService.BibleLanguage.CROATIAN
    ): BibleQuote {
        return try {
            if (language == BibleApiService.BibleLanguage.CROATIAN) {
                return getCroatianRandomQuote()
            }
            if (language == BibleApiService.BibleLanguage.GERMAN) {
                return getGermanRandomQuote()
            }
            val result = BibleApiService.getInspirationalVerse(language)
            result.getOrNull()?.let {
                BibleQuote(
                    text = it.text,
                    reference = it.reference,
                    language = language
                )
            } ?: getDefaultQuote(language = language)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            println("youtubeRating: Error getting random quote: ${e.message}")
            getDefaultQuote(language = language)
        }
    }
    
    /**
     * Dohvati citat putem API-ja (wrapper za backwards compatibility)
     */
    suspend fun getApiQuote(
        language: BibleApiService.BibleLanguage = BibleApiService.BibleLanguage.CROATIAN
    ): BibleQuote {
        return getRandomQuote(language = language)
    }
    
    /**
     * Fallback citat ako API ne radi
     */
    private fun getDefaultQuote(language: BibleApiService.BibleLanguage): BibleQuote {
        return when (language) {
            BibleApiService.BibleLanguage.CROATIAN -> BibleQuote(
                text = "Jer Bog je tako ljubio svijet da je dao svoga jedinorođenog Sina, da tko god u njega vjeruje ne propadne, nego ima život vječni.",
                reference = "Ivan 3:16",
                language = BibleApiService.BibleLanguage.CROATIAN
            )
            BibleApiService.BibleLanguage.ENGLISH -> BibleQuote(
                text = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
                reference = "John 3:16",
                language = BibleApiService.BibleLanguage.ENGLISH
            )
            BibleApiService.BibleLanguage.GERMAN -> BibleQuote(
                text = "Denn also hat Gott die Welt geliebt, dass er seinen eingeborenen Sohn gab, damit jeder, der an ihn glaubt, nicht verloren geht, sondern ewiges Leben hat.",
                reference = "Johannes 3:16",
                language = BibleApiService.BibleLanguage.GERMAN
            )
        }
    }

    private val croatianQuotes = listOf(
        BibleQuote(
            text = "Gospodin je pastir moj: ni u čem ja ne oskudijevam.",
            reference = "Psalam 23:1",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Sve mogu u Onome koji me jača.",
            reference = "Filipljanima 4:13",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Ne bojte se! Ja sam s vama u sve dane do svršetka svijeta.",
            reference = "Matej 28:20",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Mir vam ostavljam, mir vam svoj dajem.",
            reference = "Ivan 14:27",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "U početku bijaše Riječ, i Riječ bijaše u Boga, i Riječ bijaše Bog.",
            reference = "Ivan 1:1",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Blago mirotvorcima, oni će se sinovima Božjim zvati.",
            reference = "Matej 5:9",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Nada ne razočarava, jer je ljubav Božja izlivena u srca naša.",
            reference = "Rimljanima 5:5",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Tražite i naći ćete; kucajte i otvorit će vam se.",
            reference = "Matej 7:7",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Gospodin je svjetlost i spasenje moje: koga da se bojim?",
            reference = "Psalam 27:1",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Ako smo vjerni, on ostaje vjeran jer ne može sebe zanijekati.",
            reference = "2 Timoteju 2:13",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Blagoslovljen čovjek koji se uzda u Gospodina.",
            reference = "Jeremija 17:7",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "U svim se putovima svojim uzdaj u Gospodina.",
            reference = "Izreke 3:5",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Blago čistima srcem: oni će Boga gledati.",
            reference = "Matej 5:8",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Radujte se uvijek u Gospodinu; ponovit ću: radujte se!",
            reference = "Filipljanima 4:4",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Gospodin je blizu svima koji ga zazivaju.",
            reference = "Psalam 145:18",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Ne budite zabrinuti ni za što, nego u svemu molitvom i prošnjom.",
            reference = "Filipljanima 4:6",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Ljubav je strpljiva, ljubav je dobrostiva.",
            reference = "1 Korinćanima 13:4",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Gospodin će se boriti za vas; vi budite mirni.",
            reference = "Izlazak 14:14",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Dođite k meni svi koji ste umorni i opterećeni.",
            reference = "Matej 11:28",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Budi jak i hrabar! Ne boj se, jer je s tobom Gospodin.",
            reference = "Jošua 1:9",
            language = BibleApiService.BibleLanguage.CROATIAN
        ),
        BibleQuote(
            text = "Milost Gospodnja nije prestala, milosrđe njegovo nije nestalo.",
            reference = "Tužaljke 3:22",
            language = BibleApiService.BibleLanguage.CROATIAN
        )
    )

    private fun getCroatianQuoteOfTheDay(): BibleQuote {
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return croatianQuotes[dayOfYear % croatianQuotes.size]
    }

    private fun getCroatianRandomQuote(): BibleQuote {
        return croatianQuotes.random()
    }

    private val germanQuotes = listOf(
        BibleQuote(
            text = "Der HERR ist mein Hirte, mir wird nichts mangeln.",
            reference = "Psalm 23:1",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Ich vermag alles durch den, der mich mächtig macht.",
            reference = "Philipper 4:13",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Fürchte dich nicht, denn ich bin mit dir.",
            reference = "Jesaja 41:10",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Frieden hinterlasse ich euch, meinen Frieden gebe ich euch.",
            reference = "Johannes 14:27",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Im Anfang war das Wort, und das Wort war bei Gott, und das Wort war Gott.",
            reference = "Johannes 1:1",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Selig sind die Friedfertigen; denn sie werden Gottes Kinder heißen.",
            reference = "Matthäus 5:9",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Die Hoffnung lässt nicht zuschanden werden.",
            reference = "Römer 5:5",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Bittet, so wird euch gegeben; sucht, so werdet ihr finden.",
            reference = "Matthäus 7:7",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Der HERR ist mein Licht und mein Heil; vor wem sollte ich mich fürchten?",
            reference = "Psalm 27:1",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Wenn wir untreu sind, bleibt er doch treu; er kann sich selbst nicht verleugnen.",
            reference = "2 Timotheus 2:13",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Selig sind, die reinen Herzens sind; denn sie werden Gott schauen.",
            reference = "Matthäus 5:8",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Freut euch im Herrn allewege; und abermals sage ich: Freut euch!",
            reference = "Philipper 4:4",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Der HERR ist nahe allen, die ihn anrufen.",
            reference = "Psalm 145:18",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Sorgt euch um nichts, sondern bringt in allem eure Bitten vor Gott.",
            reference = "Philipper 4:6",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Die Liebe ist langmütig und freundlich.",
            reference = "1 Korinther 13:4",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Der HERR wird für euch streiten, und ihr werdet still sein.",
            reference = "2 Mose 14:14",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Kommt her zu mir, alle, die ihr mühselig und beladen seid.",
            reference = "Matthäus 11:28",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Sei stark und mutig! Fürchte dich nicht, denn der HERR, dein Gott, ist mit dir.",
            reference = "Josua 1:9",
            language = BibleApiService.BibleLanguage.GERMAN
        ),
        BibleQuote(
            text = "Die Gnade des HERRN ist nicht zu Ende, sein Erbarmen hört nicht auf.",
            reference = "Klagelieder 3:22",
            language = BibleApiService.BibleLanguage.GERMAN
        )
    )

    private fun getGermanQuoteOfTheDay(): BibleQuote {
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return germanQuotes[dayOfYear % germanQuotes.size]
    }

    private fun getGermanRandomQuote(): BibleQuote {
        return germanQuotes.random()
    }
}
