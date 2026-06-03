package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.youtube.rating.android.data.RosaryMystery
import com.youtube.rating.android.data.RosaryPosition
import com.youtube.rating.android.data.RosaryStats
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import com.youtube.rating.android.viewmodel.RosaryViewModel

/**
 * Ekran za molitvu krunice - koristi RosaryViewModel za state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosaryScreen(
    viewModel: RosaryViewModel = koinViewModel()
) {
    val context = LocalContext.current

    // Collect state from ViewModel
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val currentMystery by viewModel.currentMystery.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    var isInRoom by remember { mutableStateOf(false) }

    // Fiksna soba za zajedničku krunicu
    val ROSARY_ROOM = "ZajednickaKrunica"

    // Dohvati trenutnu dekadu i broj zrnaca
    val (currentDecade, currentBead, currentPrayer) = remember(currentPosition) {
        when (val pos = currentPosition) {
            RosaryPosition.Start -> Triple(0, 0, Strings.startRosary)
            RosaryPosition.SignOfCross -> Triple(0, 0, Strings.signOfCross)
            RosaryPosition.ApostlesCreed -> Triple(0, 0, Strings.apostlesCreed)
            RosaryPosition.FirstOurFather -> Triple(0, 0, Strings.ourFather)
            is RosaryPosition.FirstThreeHailMary -> Triple(
                0,
                pos.number,
                Strings.hailMaryCount(pos.number, 3)
            )

            RosaryPosition.FirstGloryBe -> Triple(0, 0, Strings.gloryBe)
            is RosaryPosition.Decade -> {
                val decade = pos.decadeNumber
                when (val decPos = pos.position) {
                    RosaryPosition.DecadePosition.Mystery -> Triple(
                        decade,
                        0,
                        Strings.decadeMystery(decade)
                    )

                    RosaryPosition.DecadePosition.OurFather -> Triple(decade, 0, Strings.ourFather)
                    is RosaryPosition.DecadePosition.HailMary -> Triple(
                        decade,
                        decPos.number,
                        Strings.hailMaryCount(decPos.number, 10)
                    )

                    RosaryPosition.DecadePosition.GloryBe -> Triple(decade, 10, Strings.gloryBe)
                    RosaryPosition.DecadePosition.FatimaPrayer -> Triple(
                        decade,
                        10,
                        Strings.fatimaPrayer
                    )
                }
            }

            RosaryPosition.HailHolyQueen -> Triple(5, 10, Strings.hailHolyQueen)
            RosaryPosition.Finished -> Triple(5, 10, Strings.rosaryFinished)
        }
    }

    Scaffold(
        topBar = {
            RatingTopAppBar(title = Strings.rosaryTitle)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Header s Jitsi pozivom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Strings.rosaryTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = currentMystery.getMysteryTypeName(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ako nije započeta, prikaži dugme za start
            if (currentSession == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Strings.todaysMysteries,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentMystery.getMysteryTypeName(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.startRosary() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.startRosary)
                        }
                    }
                }

                // Statistika
                Spacer(modifier = Modifier.height(16.dp))
                StatsCard(stats = stats)
            } else {
                Text(
                    text = Strings.tapForNextPrayer,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(currentPosition) {
                            detectTapGestures(
                                onTap = {
                                    if (currentPosition != RosaryPosition.Finished) {
                                        viewModel.nextStep()
                                    }
                                }
                            )
                        }
                ) {
                    // Aktivna krunica - prikaži trenutno otajstvo
                    CurrentMysteryCard(mystery = currentMystery, currentDecade = currentDecade)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trenutna molitva
                    CurrentPrayerCard(position = currentPosition, prayerName = currentPrayer)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress
                    ProgressIndicator(currentDecade = currentDecade, currentBead = currentBead)
                }
            }
        }
    }
}

@Composable
private fun CurrentMysteryCard(mystery: RosaryMystery, currentDecade: Int) {
    if (currentDecade in 1..5) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.decadeMystery(currentDecade),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mystery.getMysteryTitle(currentDecade - 1),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mystery.getMysteryDescription(currentDecade - 1),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun CurrentPrayerCard(position: RosaryPosition, prayerName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "🙏 $prayerName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = getPrayerText(position = position),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 28.sp
            )
        }
    }
}

@Composable
private fun ProgressIndicator(currentDecade: Int, currentBead: Int) {
    val progress = when {
        currentDecade == 0 -> 0f
        currentDecade <= 5 -> ((currentDecade - 1) * 10 + currentBead) / 50f
        else -> 1f
    }

    Column {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Napredak: ${(progress * 50).toInt()} / 50",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsCard(stats: RosaryStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "🔥 Niz", value = "${stats.currentStreak} dana")
            StatItem(label = "📿 Ukupno", value = "${stats.completedSessions}")
            StatItem(label = "� Sesije", value = "${stats.totalSessions}")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * Dohvati tekst molitve za trenutnu poziciju
 */
private fun getPrayerText(position: RosaryPosition): String {
    return when (position) {
        RosaryPosition.Start -> "U ime Oca i Sina i Duha Svetoga. Amen."
        RosaryPosition.SignOfCross -> "U ime Oca i Sina i Duha Svetoga. Amen."
        RosaryPosition.ApostlesCreed -> "Vjerujem u Boga, Oca svemogućega, Stvoritelja neba i zemlje. I u Isusa Krista, Sina njegova jedinoga, Gospodina našega, koji je začet od Duha Svetoga, rođen od Marije Djevice..."
        RosaryPosition.FirstOurFather -> "Oče naš, koji jesi na nebesima, sveti se ime tvoje, dođi kraljevstvo tvoje, budi volja tvoja, kako na nebu tako i na zemlji. Kruh naš svagdanji daj nam danas, i otpusti nam duge naše, kako i mi otpuštamo dužnicima našim, i ne uvedi nas u napast, nego izbavi nas od zla. Amen."
        is RosaryPosition.FirstThreeHailMary -> "Zdravo Marijo, milosti puna, Gospodin s tobom, blagoslovljena ti među ženama i blagoslovljen plod utrobe tvoje, Isus. Sveta Marijo, Majko Božja, moli za nas grešnike, sada i na čas smrti naše. Amen."
        RosaryPosition.FirstGloryBe -> "Slava Ocu i Sinu i Duhu Svetomu. Kako bijaše na početku, tako i sada i vazda i u vijeke vjekova. Amen."
        is RosaryPosition.Decade -> {
            when (position.position) {
                RosaryPosition.DecadePosition.Mystery -> "Razmišljamo o otajstvu..."
                RosaryPosition.DecadePosition.OurFather -> "Oče naš, koji jesi na nebesima, sveti se ime tvoje, dođi kraljevstvo tvoje, budi volja tvoja, kako na nebu tako i na zemlji. Kruh naš svagdanji daj nam danas, i otpusti nam duge naše, kako i mi otpuštamo dužnicima našim, i ne uvedi nas u napast, nego izbavi nas od zla. Amen."
                is RosaryPosition.DecadePosition.HailMary -> "Zdravo Marijo, milosti puna, Gospodin s tobom, blagoslovljena ti među ženama i blagoslovljen plod utrobe tvoje, Isus. Sveta Marijo, Majko Božja, moli za nas grešnike, sada i na čas smrti naše. Amen."
                RosaryPosition.DecadePosition.GloryBe -> "Slava Ocu i Sinu i Duhu Svetomu. Kako bijaše na početku, tako i sada i vazda i u vijeke vjekova. Amen."
                RosaryPosition.DecadePosition.FatimaPrayer -> "O moj Isuse, oprosti nam naše grijehe, sačuvaj nas od paklenog ognja i povedi u nebo sve duše, osobito one kojima je najpotrebnija tvoja pomoć. Amen."
            }
        }

        RosaryPosition.HailHolyQueen -> "Zdravo Kraljice, Majko milosrđa, živote, slasti i nado naša, zdravo! K tebi vapijemo prognani sinovi Evini..."
        RosaryPosition.Finished -> "Krunica je završena! Hvaljen Isus i Marija!"
    }
}

/**
 * Extension funkcije za RosaryMystery
 */
private fun RosaryMystery.getMysteryTypeName(): String {
    return when (this) {
        RosaryMystery.JOYFUL -> "Radosna otajstva"
        RosaryMystery.LUMINOUS -> "Svjetlosna otajstva"
        RosaryMystery.SORROWFUL -> "Žalosna otajstva"
        RosaryMystery.GLORIOUS -> "Slavna otajstva"
    }
}

private fun RosaryMystery.getMysteryTitle(index: Int): String {
    val titles = when (this) {
        RosaryMystery.JOYFUL -> listOf(
            "Navještenje",
            "Pohođenje",
            "Rođenje Isusovo",
            "Prikazanje u Hramu",
            "Nalaženje u Hramu"
        )

        RosaryMystery.LUMINOUS -> listOf(
            "Krštenje Isusovo",
            "Kana Galilejska",
            "Navještaj Kraljevstva",
            "Preobraženje",
            "Ustanova Euharistije"
        )

        RosaryMystery.SORROWFUL -> listOf(
            "Molitva u vrtu",
            "Bičevanje",
            "Trnovita kruna",
            "Nošenje križa",
            "Raspeće"
        )

        RosaryMystery.GLORIOUS -> listOf(
            "Uskrsnuće",
            "Uzašašće",
            "Silazak Duha Svetoga",
            "Uznesenje Marijino",
            "Kruništvo Marijino"
        )
    }
    return titles.getOrNull(index) ?: ""
}

private fun RosaryMystery.getMysteryDescription(index: Int): String {
    val descriptions = when (this) {
        RosaryMystery.JOYFUL -> listOf(
            "Anđeo Gabrijel navješćuje Mariji da će začeti Sina Božjega",
            "Marija pohodi svoju rođakinju Elizabetu",
            "Isus se rađa u Betlehemu",
            "Marija i Josip prikazuju Isusa u Hramu",
            "Marija i Josip nalaze dvanaestogodišnjeg Isusa u Hramu"
        )

        RosaryMystery.LUMINOUS -> listOf(
            "Ivan kršćava Isusa u rijeci Jordan",
            "Isus učini svoje prvo čudo na svadbi u Kani",
            "Isus navješćuje Kraljevstvo Božje i poziva na obraćenje",
            "Isus se preobražava pred učenicima na gori Tabor",
            "Isus ustanovljuje Euharistiju na Posljednjoj večeri"
        )

        RosaryMystery.SORROWFUL -> listOf(
            "Isus se moli u Getsemanskom vrtu",
            "Isus je bičevan i izrugan",
            "Isus je okrunjen trnovom krunom",
            "Isus nosi križ na Kalvariju",
            "Isus umire na križu"
        )

        RosaryMystery.GLORIOUS -> listOf(
            "Isus uskrsava od mrtvih trećeg dana",
            "Isus uzlazi na nebesa",
            "Duh Sveti silazi na apostole",
            "Marija uznesena je na nebesa s dušom i tijelom",
            "Marija je okrunjena kao Kraljica neba i zemlje"
        )
    }
    return descriptions.getOrNull(index) ?: ""
}
