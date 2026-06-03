package com.youtube.rating.android.data

import java.util.Calendar

/**
 * Otajstva krunice (Mysteries of the Rosary)
 */
enum class RosaryMystery(val title: String, val mysteries: List<String>) {
    JOYFUL(
        title = "Radosna otajstva",
        mysteries = listOf(
            "Navještenje Marijino",
            "Pohođenje Elizabete",
            "Rođenje Isusovo",
            "Prinošenje Isusovo u hramu",
            "Pronalazak Isusa u hramu"
        )
    ),
    LUMINOUS(
        title = "Otajstva svjetla",
        mysteries = listOf(
            "Krštenje Isusovo na Jordanu",
            "Isusovo samootkrivenje na svadbi u Kani",
            "Navještaj Kraljevstva Božjega",
            "Preobraženje Gospodinovo",
            "Ustanovljenje Euharistije"
        )
    ),
    SORROWFUL(
        title = "Žalosna otajstva",
        mysteries = listOf(
            "Isusova molitva u Getsemanskom vrtu",
            "Isusovo bičevanje",
            "Trnovanjem Isusovim",
            "Nošenje križa",
            "Razapinjanje i smrt Isusova"
        )
    ),
    GLORIOUS(
        title = "Slavna otajstva",
        mysteries = listOf(
            "Uskrsnuće Isusovo",
            "Uzašašće Isusovo na nebo",
            "Silazak Duha Svetoga",
            "Uznesenje Marijino na nebo",
            "Krunisanje Marijino"
        )
    );

    companion object {
        /**
         * Vraća otajstva prema danu u tjednu
         * Ponedjeljak i Subota: Radosna
         * Utorak i Petak: Žalosna
         * Srijeda: Slavna
         * Četvrtak: Otajstva svjetla
         * Nedjelja: Slavna (ili Radosna u Adventu/Božiću)
         */
        fun forToday(): RosaryMystery {
            val calendar = Calendar.getInstance()
            return when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY, Calendar.SATURDAY -> JOYFUL
                Calendar.TUESDAY, Calendar.FRIDAY -> SORROWFUL
                Calendar.WEDNESDAY -> GLORIOUS
                Calendar.THURSDAY -> LUMINOUS
                Calendar.SUNDAY -> GLORIOUS // Može se dodati logika za Advent/Božić
                else -> JOYFUL
            }
        }

        fun forDay(dayOfWeek: Int): RosaryMystery {
            return when (dayOfWeek) {
                Calendar.MONDAY, Calendar.SATURDAY -> JOYFUL
                Calendar.TUESDAY, Calendar.FRIDAY -> SORROWFUL
                Calendar.WEDNESDAY -> GLORIOUS
                Calendar.THURSDAY -> LUMINOUS
                Calendar.SUNDAY -> GLORIOUS
                else -> JOYFUL
            }
        }
    }
}

/**
 * Molitva krunice
 */
enum class RosaryPrayer(val title: String, val text: String) {
    SIGN_OF_CROSS(
        "Znak križa",
        "U ime Oca i Sina i Duha Svetoga. Amen."
    ),
    APOSTLES_CREED(
        "Vjerovanje",
        "Vjerujem u Boga, Oca svemogućega, Stvoritelja neba i zemlje.\n\n" +
                "I u Isusa Krista, Sina njegova jedinoga, Gospodina našega,\n" +
                "koji je začet po Duhu Svetom, rođen od Marije Djevice,\n" +
                "mučen pod Poncijom Pilatom, raspet, umro i pokopan,\n" +
                "sišao u carstvo smrti, treći dan uskrsnuo od mrtvih,\n" +
                "uzašao na nebo, sjedi o desnu Boga Oca svemogućega,\n" +
                "odande će doći suditi žive i mrtve.\n\n" +
                "Vjerujem u Duha Svetoga,\n" +
                "svetu Crkvu katoličku, zajednicu svetih,\n" +
                "oproštenje grijeha,\n" +
                "uskrsnuće tijela i život vječni. Amen."
    ),
    OUR_FATHER(
        "Oče naš",
        "Oče naš, koji jesi na nebesima,\n" +
                "sveti se ime tvoje,\n" +
                "dođi kraljevstvo tvoje,\n" +
                "budi volja tvoja kako na nebu tako i na zemlji.\n" +
                "Kruh naš svagdanji daj nam danas\n" +
                "i otpusti nam duge naše\n" +
                "kako i mi otpuštamo dužnicima našim\n" +
                "i ne uvedi nas u napast,\n" +
                "nego izbavi nas od zla. Amen."
    ),
    HAIL_MARY(
        "Zdravo Marijo",
        "Zdravo, Marijo, milosti puna, Gospodin s tobom,\n" +
                "blagoslovljena ti među ženama\n" +
                "i blagoslovljen plod utrobe tvoje, Isus.\n" +
                "Sveta Marijo, Majko Božja,\n" +
                "moli za nas grešnike,\n" +
                "sada i na času smrti naše. Amen."
    ),
    GLORY_BE(
        "Slava Ocu",
        "Slava Ocu i Sinu i Duhu Svetomu.\n" +
                "Kao što bijaše na početku, sada i vazda\n" +
                "i u vijeke vjekova. Amen."
    ),
    FATIMA_PRAYER(
        "Fatimska molitva",
        "O moj Isuse, oprosti nam grijehe naše,\n" +
                "sačuvaj nas od paklenoga ognja,\n" +
                "dovedi sve duše na nebo,\n" +
                "a poglavito one kojima je potrebnija tvoja milost. Amen."
    ),
    HAIL_HOLY_QUEEN(
        "Pod tvoje se okrilje",
        "Pod tvoje se okrilje utječemo,\n" +
                "sveta Bogorodice!\n" +
                "Naše molbe nemoj prezreti u potrebama našim,\n" +
                "nego od svih nas nevolja\n" +
                "izbavi nas uvijek,\n" +
                "Djevice slave i blagoslova.\n" +
                "O, blaga, o, dobrostiva,\n" +
                "o, slatka Djevice Marijo! Amen."
    )
}

/**
 * Pozicija u molitvi krunice
 */
sealed class RosaryPosition {
    object Start : RosaryPosition()
    object SignOfCross : RosaryPosition()
    object ApostlesCreed : RosaryPosition()
    object FirstOurFather : RosaryPosition()
    data class FirstThreeHailMary(val number: Int) : RosaryPosition() // 1-3
    object FirstGloryBe : RosaryPosition()
    data class Decade(val decadeNumber: Int, val position: DecadePosition) : RosaryPosition() // 1-5
    object HailHolyQueen : RosaryPosition()
    object Finished : RosaryPosition()

    sealed class DecadePosition {
        object Mystery : DecadePosition()
        object OurFather : DecadePosition()
        data class HailMary(val number: Int) : DecadePosition() // 1-10
        object GloryBe : DecadePosition()
        object FatimaPrayer : DecadePosition()
    }
}

/**
 * Zapis molitvi krunice
 */
data class RosarySession(
    val id: String,
    val date: Long, // timestamp
    val mysteryType: String, // RosaryMystery name
    val completed: Boolean,
    val startTime: Long,
    val endTime: Long? = null,
    val decadesCompleted: Int = 0
)

/**
 * Postavke za notifikacije krunice
 */
data class RosaryNotificationSettings(
    val enabled: Boolean = false,
    val hour: Int = 20, // 8 PM default
    val minute: Int = 0,
    val daysOfWeek: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    )
)

/**
 * Statistika molitvi krunice
 */
data class RosaryStats(
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val currentStreak: Int = 0, // koliko uzastopnih dana
    val longestStreak: Int = 0,
    val lastPrayedDate: Long? = null,
    val sessionsPerMystery: Map<String, Int> = emptyMap() // broj molitvi po tipu otajstva
)
