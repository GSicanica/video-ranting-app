package com.youtube.rating.android.data

/**
 * Lista svih knjiga Biblije s brojem poglavlja
 * Koristi se za sekvencijalno čitanje Biblije od početka do kraja
 */
object BibleBooks {

    data class BibleBook(
        val id: String,           // ID za API (npr. "postanak", "izlazak")
        val name: String,         // Hrvatski naziv
        val chapters: Int,        // Broj poglavlja
        val testament: Testament
    )

    enum class Testament {
        OLD, NEW
    }

    val books: List<BibleBook> = listOf(
        // Stari zavjet - Petoknjižje
        BibleBook("postanak", "Postanak", 50, Testament.OLD),
        BibleBook("izlazak", "Izlazak", 40, Testament.OLD),
        BibleBook("levitski-zakonik", "Levitski zakonik", 27, Testament.OLD),
        BibleBook("brojevi", "Brojevi", 36, Testament.OLD),
        BibleBook("ponovljeni-zakon", "Ponovljeni zakon", 34, Testament.OLD),

        // Stari zavjet - Povijesne knjige
        BibleBook("josua", "Jošua", 24, Testament.OLD),
        BibleBook("suci", "Suci", 21, Testament.OLD),
        BibleBook("ruta", "Ruta", 4, Testament.OLD),
        BibleBook("1-samuelova", "1. Samuelova", 31, Testament.OLD),
        BibleBook("2-samuelova", "2. Samuelova", 24, Testament.OLD),
        BibleBook("1-kraljeva", "1. Kraljeva", 22, Testament.OLD),
        BibleBook("2-kraljeva", "2. Kraljeva", 25, Testament.OLD),
        BibleBook("1-ljetopisa", "1. Ljetopisa", 29, Testament.OLD),
        BibleBook("2-ljetopisa", "2. Ljetopisa", 36, Testament.OLD),
        BibleBook("ezra", "Ezra", 10, Testament.OLD),
        BibleBook("nehemija", "Nehemija", 13, Testament.OLD),
        BibleBook("estera", "Estera", 10, Testament.OLD),

        // Stari zavjet - Mudrosne knjige
        BibleBook("job", "Job", 42, Testament.OLD),
        BibleBook("psalmi", "Psalmi", 150, Testament.OLD),
        BibleBook("mudre-izreke", "Mudre izreke", 31, Testament.OLD),
        BibleBook("propovjednik", "Propovjednik", 12, Testament.OLD),
        BibleBook("pjesma-nad-pjesmama", "Pjesma nad pjesmama", 8, Testament.OLD),

        // Stari zavjet - Veliki proroci
        BibleBook("izaija", "Izaija", 66, Testament.OLD),
        BibleBook("jeremija", "Jeremija", 52, Testament.OLD),
        BibleBook("tuzaljke", "Tužaljke", 5, Testament.OLD),
        BibleBook("ezekiel", "Ezekiel", 48, Testament.OLD),
        BibleBook("daniel", "Daniel", 12, Testament.OLD),

        // Stari zavjet - Mali proroci
        BibleBook("hosea", "Hošea", 14, Testament.OLD),
        BibleBook("joel", "Joel", 3, Testament.OLD),
        BibleBook("amos", "Amos", 9, Testament.OLD),
        BibleBook("obadija", "Obadija", 1, Testament.OLD),
        BibleBook("jona", "Jona", 4, Testament.OLD),
        BibleBook("mihej", "Mihej", 7, Testament.OLD),
        BibleBook("nahum", "Nahum", 3, Testament.OLD),
        BibleBook("habakuk", "Habakuk", 3, Testament.OLD),
        BibleBook("sefanija", "Sefanija", 3, Testament.OLD),
        BibleBook("hagaj", "Hagaj", 2, Testament.OLD),
        BibleBook("zaharija", "Zaharija", 14, Testament.OLD),
        BibleBook("malahija", "Malahija", 4, Testament.OLD),

        // Novi zavjet - Evanđelja
        BibleBook("matej", "Evanđelje po Mateju", 28, Testament.NEW),
        BibleBook("marko", "Evanđelje po Marku", 16, Testament.NEW),
        BibleBook("luka", "Evanđelje po Luki", 24, Testament.NEW),
        BibleBook("ivan", "Evanđelje po Ivanu", 21, Testament.NEW),

        // Novi zavjet - Djela apostolska
        BibleBook("djela", "Djela apostolska", 28, Testament.NEW),

        // Novi zavjet - Pavlove poslanice
        BibleBook("rimljanima", "Poslanica Rimljanima", 16, Testament.NEW),
        BibleBook("1-korincanima", "1. Korinćanima", 16, Testament.NEW),
        BibleBook("2-korincanima", "2. Korinćanima", 13, Testament.NEW),
        BibleBook("galacanima", "Galaćanima", 6, Testament.NEW),
        BibleBook("efezanima", "Efežanima", 6, Testament.NEW),
        BibleBook("filipljanima", "Filipljanima", 4, Testament.NEW),
        BibleBook("kolosanima", "Kološanima", 4, Testament.NEW),
        BibleBook("1-solunjanima", "1. Solunjanima", 5, Testament.NEW),
        BibleBook("2-solunjanima", "2. Solunjanima", 3, Testament.NEW),
        BibleBook("1-timoteju", "1. Timoteju", 6, Testament.NEW),
        BibleBook("2-timoteju", "2. Timoteju", 4, Testament.NEW),
        BibleBook("titu", "Titu", 3, Testament.NEW),
        BibleBook("filemonu", "Filemonu", 1, Testament.NEW),

        // Novi zavjet - Ostale poslanice
        BibleBook("hebrejima", "Hebrejima", 13, Testament.NEW),
        BibleBook("jakovljeva", "Jakovljeva poslanica", 5, Testament.NEW),
        BibleBook("1-petrova", "1. Petrova", 5, Testament.NEW),
        BibleBook("2-petrova", "2. Petrova", 3, Testament.NEW),
        BibleBook("1-ivanova", "1. Ivanova", 5, Testament.NEW),
        BibleBook("2-ivanova", "2. Ivanova", 1, Testament.NEW),
        BibleBook("3-ivanova", "3. Ivanova", 1, Testament.NEW),
        BibleBook("judina", "Judina poslanica", 1, Testament.NEW),

        // Novi zavjet - Otkrivenje
        BibleBook("otkrivenje", "Otkrivenje", 22, Testament.NEW)
    )

    val totalBooks: Int = books.size
    val totalChapters: Int = books.sumOf { it.chapters }

    fun getBook(index: Int): BibleBook? = books.getOrNull(index)

    fun getBookById(id: String): BibleBook? = books.find { it.id == id }

    /**
     * Izračunaj ukupni napredak (0.0 - 1.0) na temelju trenutne pozicije
     */
    fun calculateProgress(bookIndex: Int, chapter: Int): Float {
        if (bookIndex < 0 || bookIndex >= books.size) return 0f

        var completedChapters = 0
        for (i in 0 until bookIndex) {
            completedChapters += books[i].chapters
        }
        completedChapters += (chapter - 1).coerceAtLeast(0)

        return completedChapters.toFloat() / totalChapters.toFloat()
    }

    /**
     * Dohvati sljedeće poglavlje (book index, chapter)
     * Vraća null ako smo na kraju Biblije
     */
    fun getNextChapter(bookIndex: Int, chapter: Int): Pair<Int, Int>? {
        val currentBook = books.getOrNull(bookIndex) ?: return null

        return if (chapter < currentBook.chapters) {
            // Sljedeće poglavlje iste knjige
            Pair(bookIndex, chapter + 1)
        } else if (bookIndex < books.size - 1) {
            // Prva poglavlje sljedeće knjige
            Pair(bookIndex + 1, 1)
        } else {
            // Kraj Biblije
            null
        }
    }

    /**
     * Dohvati prethodno poglavlje (book index, chapter)
     * Vraća null ako smo na početku Biblije
     */
    fun getPreviousChapter(bookIndex: Int, chapter: Int): Pair<Int, Int>? {
        return if (chapter > 1) {
            // Prethodno poglavlje iste knjige
            Pair(bookIndex, chapter - 1)
        } else if (bookIndex > 0) {
            // Zadnje poglavlje prethodne knjige
            val prevBook = books[bookIndex - 1]
            Pair(bookIndex - 1, prevBook.chapters)
        } else {
            // Početak Biblije
            null
        }
    }
}
