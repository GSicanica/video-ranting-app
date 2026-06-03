package com.youtube.rating.android

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.youtube.rating.android.data.prefs.TrainingPrefs
import com.youtube.rating.android.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

internal fun isTrainingRoute(route: String?): Boolean =
    route == Screen.Training.route ||
        route == Screen.Bible.route ||
        (route?.startsWith("bible/") == true) ||
        (route?.startsWith("reader/") == true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RatingAppBackHandler(
    activity: Activity?,
    navController: NavHostController,
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    scope: CoroutineScope
) {
    BackHandler(enabled = true) {
        scope.launch {
            if (leftDrawerState.isOpen) {
                leftDrawerState.close()
            } else if (rightDrawerState.isOpen) {
                rightDrawerState.close()
            } else if (navController.popBackStack()) {
                Unit
            } else {
                activity?.finish()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberTrainingPsalmsToday(
    context: Context,
    isTrainingTab: Boolean,
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    scope: CoroutineScope
): List<Int> {
    var trainingPsalmsToday by remember { mutableStateOf<List<Int>>(emptyList()) }

    LaunchedEffect(isTrainingTab) {
        if (!isTrainingTab) return@LaunchedEffect
        scope.launch {
            leftDrawerState.close()
            rightDrawerState.close()
        }

        val stored = TrainingPrefs.getTrainingPsalmsToday(context)
        val parsed = stored.mapNotNull { it.toIntOrNull() }
            .filter { it in 1..150 }
            .distinct()
        val selectedPsalms = if (parsed.size >= 3) {
            parsed.take(3)
        } else {
            val seed = LocalDate.now().toString().hashCode()
            val random = Random(seed)
            val selected = LinkedHashSet<Int>(3)
            while (selected.size < 3) {
                selected.add(random.nextInt(1, 151))
            }
            selected.toList()
        }.sorted()

        trainingPsalmsToday = selectedPsalms
        if (parsed.size < 3) {
            TrainingPrefs.setTrainingPsalmsToday(
                context,
                selectedPsalms.map { it.toString() }.toSet()
            )
        }
    }

    return trainingPsalmsToday
}
