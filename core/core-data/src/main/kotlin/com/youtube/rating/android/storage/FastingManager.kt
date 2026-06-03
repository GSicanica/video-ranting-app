package com.youtube.rating.android.storage

import android.content.Context
import com.youtube.rating.android.utils.BackupTrigger
import com.youtube.rating.android.utils.FastingReminderService
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedHashMap
import org.koin.core.context.GlobalContext
import com.youtube.rating.android.data.prefs.FastingPrefs

class FastingManager private constructor(private val context: Context) {
    private val backupTrigger: BackupTrigger by lazy {
        GlobalContext.get().get()
    }
    private val reminderService: FastingReminderService by lazy {
        GlobalContext.get().get()
    }
    suspend fun getEntries(): Map<String, FastingEntry> {
        val json = getEntriesJsonWithMigration()
        return parseEntries(json = json)
    }

    suspend fun exportEntries(): JSONArray {
        val arr = JSONArray()
        val entries = getEntries().values.sortedBy { it.dateKey }
        entries.forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("dateKey", entry.dateKey)
                    put("type", entry.type)
                    put("durationHours", entry.durationHours)
                    put("note", entry.note)
                    put("isFasting", entry.isFasting)
                    put("updatedAt", entry.updatedAt)
                }
            )
        }
        return arr
    }

    suspend fun importEntries(entries: JSONArray): Int {
        val parsed = parseEntries(json = entries.toString())
        saveEntries(entries = parsed.values.toList())
        return parsed.size
    }

    suspend fun getEntry(dateKey: String): FastingEntry? = getEntries()[dateKey]

    suspend fun toggleDay(dateKey: String): Map<String, FastingEntry> {
        val current = getEntries().toMutableMap()
        if (current.containsKey(dateKey)) {
            current.remove(dateKey)
        } else {
            current[dateKey] = FastingEntry(dateKey = dateKey)
        }
        saveEntries(entries = current.values.toList())
        return current
    }

    suspend fun upsertEntry(entry: FastingEntry): Map<String, FastingEntry> {
        val current = getEntries().toMutableMap()
        if (!entry.isFasting) {
            current.remove(entry.dateKey)
        } else {
            current[entry.dateKey] = entry.copy(updatedAt = System.currentTimeMillis())
        }
        saveEntries(entries = current.values.toList())
        return current
    }

    suspend fun removeEntry(dateKey: String): Map<String, FastingEntry> {
        val current = getEntries().toMutableMap()
        current.remove(dateKey)
        saveEntries(entries = current.values.toList())
        return current
    }

    suspend fun getWeeklyGoal(): Int {
        migrateLegacyIfNeeded()
        return FastingPrefs.getFastingWeeklyGoal(context, DEFAULT_WEEKLY_GOAL)
    }

    suspend fun setWeeklyGoal(goal: Int) {
        val value = goal.coerceIn(1, 7)
        FastingPrefs.setFastingWeeklyGoal(context, value)
    }

    suspend fun getReminderSettings(): FastingReminderSettings {
        migrateLegacyIfNeeded()
        return FastingReminderSettings(
            enabled = FastingPrefs.getFastingReminderEnabled(context),
            hour = FastingPrefs.getFastingReminderHour(context),
            minute = FastingPrefs.getFastingReminderMinute(context)
        )
    }

    suspend fun saveReminderSettings(settings: FastingReminderSettings) {
        val hour = settings.hour.coerceIn(0, 23)
        val minute = settings.minute.coerceIn(0, 59)
        FastingPrefs.setFastingReminder(context, settings.enabled, hour, minute)
        reminderService.schedule(context, settings.copy(hour = hour, minute = minute))
    }

    private suspend fun saveEntries(entries: List<FastingEntry>) {
        val arr = JSONArray()
        entries.sortedBy { it.dateKey }.forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("dateKey", entry.dateKey)
                    put("type", entry.type)
                    put("durationHours", entry.durationHours.coerceAtLeast(1))
                    put("note", entry.note)
                    put("isFasting", entry.isFasting)
                    put("updatedAt", entry.updatedAt)
                }
            )
        }
        FastingPrefs.setFastingEntriesJson(context, arr.toString())
        try {
            backupTrigger.trigger(com.youtube.rating.android.utils.ChangeType.FASTING_UPDATED)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        }
    }

    private suspend fun getEntriesJsonWithMigration(): String {
        migrateLegacyIfNeeded()
        return FastingPrefs.getFastingEntriesJson(context)
    }

    private suspend fun migrateLegacyIfNeeded() {
        val stored = FastingPrefs.getFastingEntriesJson(context)
        if (stored != "[]") return

        val legacyPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val legacyEntries = legacyPrefs.getString(KEY_FASTING_ENTRIES, "[]") ?: "[]"
        val legacyGoal = legacyPrefs.getInt(KEY_WEEKLY_GOAL, DEFAULT_WEEKLY_GOAL)
        val legacyEnabled = legacyPrefs.getBoolean(KEY_REMINDER_ENABLED, false)
        val legacyHour = legacyPrefs.getInt(KEY_REMINDER_HOUR, 6)
        val legacyMinute = legacyPrefs.getInt(KEY_REMINDER_MINUTE, 0)

        FastingPrefs.setFastingEntriesJson(context, legacyEntries)
        FastingPrefs.setFastingWeeklyGoal(context, legacyGoal)
        FastingPrefs.setFastingReminder(context, legacyEnabled, legacyHour, legacyMinute)
    }

    private fun parseEntries(json: String): Map<String, FastingEntry> {
        return try {
            val arr = JSONArray(json)
            val result = LinkedHashMap<String, FastingEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val dateKey = obj.optString("dateKey")
                if (dateKey.isBlank()) continue
                result[dateKey] = FastingEntry(
                    dateKey = dateKey,
                    type = obj.optString("type", FASTING_TYPE_WATER),
                    durationHours = obj.optInt("durationHours", 16),
                    note = obj.optString("note", ""),
                    isFasting = obj.optBoolean("isFasting", true),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            }
            result
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            emptyMap()
        }
    }

    companion object {
        private const val PREFS_NAME = "fasting_prefs"
        private const val KEY_FASTING_ENTRIES = "fasting_entries"
        private const val KEY_WEEKLY_GOAL = "fasting_weekly_goal"
        private const val KEY_REMINDER_ENABLED = "fasting_reminder_enabled"
        private const val KEY_REMINDER_HOUR = "fasting_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "fasting_reminder_minute"

        private const val DEFAULT_WEEKLY_GOAL = 3

        @Volatile
        private var instance: FastingManager? = null

        fun getInstance(context: Context): FastingManager {
            return instance ?: synchronized(this) {
                instance ?: FastingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
