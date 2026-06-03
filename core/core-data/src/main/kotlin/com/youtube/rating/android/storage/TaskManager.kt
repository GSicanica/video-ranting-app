package com.youtube.rating.android.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.youtube.rating.android.data.prefs.TasksPrefs

data class TaskItem(
    val id: Long,
    val title: String,
    val isDone: Boolean,
    val pinned: Boolean = false,
    /**
     * Manual ordering key (higher = shown earlier when using manual sort).
     * Persisted to tasks_json; older data will default to createdAt.
     */
    val order: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class TaskManager(private val context: Context) {
    suspend fun getTasks(): List<TaskItem> {
        val stored = TasksPrefs.getTasksJson(context)
        val json = if (stored == "[]") {
            val legacy = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_TASKS, "[]") ?: "[]"
            if (legacy != "[]") {
                TasksPrefs.setTasksJson(context, legacy)
                legacy
            } else {
                stored
            }
        } else {
            stored
        }
        return parseTasks(json = json)
    }

    suspend fun saveTasks(tasks: List<TaskItem>) {
        val json = tasksToJson(tasks = tasks).toString()
        TasksPrefs.setTasksJson(context, json)
    }

    suspend fun exportTasks(): JSONArray = tasksToJson(tasks = getTasks())

    suspend fun importTasks(tasksArray: JSONArray): Int {
        val tasks = parseTasks(json = tasksArray.toString())
        saveTasks(tasks = tasks)
        return tasks.size
    }

    private fun parseTasks(json: String): List<TaskItem> {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<TaskItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    TaskItem(
                        id = obj.optLong("id"),
                        title = obj.optString("title"),
                        isDone = obj.optBoolean("isDone", false),
                        pinned = obj.optBoolean("pinned", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        order = obj.optLong("order", obj.optLong("createdAt", System.currentTimeMillis()))
                    )
                )
            }
            list
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            emptyList()
        }
    }

    private fun tasksToJson(tasks: List<TaskItem>): JSONArray {
        val arr = JSONArray()
        tasks.forEach { task ->
            arr.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("isDone", task.isDone)
                    put("pinned", task.pinned)
                    put("order", task.order)
                    put("createdAt", task.createdAt)
                    put("updatedAt", task.updatedAt)
                }
            )
        }
        return arr
    }

    companion object {
        private const val PREFS_NAME = "tasks_prefs"
        private const val KEY_TASKS = "tasks_json"
    }
}