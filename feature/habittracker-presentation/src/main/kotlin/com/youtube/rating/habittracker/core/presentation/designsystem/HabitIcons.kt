package com.youtube.rating.habittracker.core.presentation.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.youtube.rating.habittracker.presentation.R
import com.youtube.rating.habittracker.feature.habits.domain.HabitIcon

object HabitIcons {

    @DrawableRes
    fun resourceFor(icon: HabitIcon): Int {
        return when (icon) {
            HabitIcon.RUN -> R.drawable.ic_habit_run
            HabitIcon.READ -> R.drawable.ic_habit_read
            HabitIcon.WATER -> R.drawable.ic_habit_water
            HabitIcon.MEDITATE -> R.drawable.ic_habit_meditate
            HabitIcon.SLEEP -> R.drawable.ic_habit_sleep
            HabitIcon.CODE -> R.drawable.ic_habit_code
            HabitIcon.MUSIC -> R.drawable.ic_habit_music
            HabitIcon.COOK -> R.drawable.ic_habit_cook
            HabitIcon.JOURNAL -> R.drawable.ic_habit_journal
            HabitIcon.GYM -> R.drawable.ic_habit_gym
            HabitIcon.VJEZBA -> R.drawable.ic_habit_vjezbanje
            HabitIcon.WALK -> R.drawable.ic_habit_walk
            HabitIcon.CYCLE -> R.drawable.ic_habit_cycle
            HabitIcon.STUDY -> R.drawable.ic_habit_study
            HabitIcon.NO_PHONE -> R.drawable.ic_habit_no_phone
            HabitIcon.VITAMINS -> R.drawable.ic_habit_vitamins
            HabitIcon.LANGUAGE -> R.drawable.ic_habit_language
            HabitIcon.GRATITUDE -> R.drawable.ic_habit_gratitude
            HabitIcon.HEALTH -> R.drawable.ic_habit_health
            HabitIcon.ORGANIZE -> R.drawable.ic_habit_organize
        }
    }

    @Composable
    fun painter(icon: HabitIcon): Painter {
        return painterResource(resourceFor(icon))
    }
}
