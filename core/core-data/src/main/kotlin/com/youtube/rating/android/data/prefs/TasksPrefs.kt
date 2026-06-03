package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow

object TasksPrefs : BasePrefs() {
    private val KEY_TASKS_JSON = stringPreferencesKey("tasks_json")
    private val KEY_TASK_DRAWER_HIDE_DONE = booleanPreferencesKey("task_drawer_hide_done")
    private val KEY_TASK_DRAWER_SORT = stringPreferencesKey("task_drawer_sort")

    suspend fun getTasksJson(context: Context): String =
        readPref(context, KEY_TASKS_JSON, "[]")

    fun tasksJsonFlow(context: Context): Flow<String> =
        prefsFlow(context, KEY_TASKS_JSON, "[]")

    suspend fun setTasksJson(context: Context, json: String) {
        editPref(context) { it[KEY_TASKS_JSON] = json }
    }

    fun taskDrawerHideDoneFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_TASK_DRAWER_HIDE_DONE, false)

    suspend fun setTaskDrawerHideDone(context: Context, hideDone: Boolean) {
        editPref(context) { it[KEY_TASK_DRAWER_HIDE_DONE] = hideDone }
    }

    fun taskDrawerSortFlow(context: Context): Flow<String> =
        prefsFlow(context, KEY_TASK_DRAWER_SORT, "NEWEST")

    suspend fun setTaskDrawerSort(context: Context, sort: String) {
        editPref(context) { it[KEY_TASK_DRAWER_SORT] = sort }
    }
}