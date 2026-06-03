package com.youtube.rating.habittracker.feature.habits.data.di

import android.content.Context
import androidx.room.Room
import com.youtube.rating.habittracker.feature.habits.data.HabitDatabase
import com.youtube.rating.habittracker.feature.habits.data.RoomHabitDataSource
import com.youtube.rating.habittracker.feature.habits.domain.HabitLocalDataSource
import org.koin.dsl.module

val habitsDataModule = module {
    single {
        Room.databaseBuilder(
            get<Context>(),
            HabitDatabase::class.java,
            "habits.db"
        ).build()
    }
    single { get<HabitDatabase>().habitDao }
    single<HabitLocalDataSource> { RoomHabitDataSource(get()) }
}
