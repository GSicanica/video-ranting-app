package com.youtube.rating.android.storage

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import com.youtube.rating.android.data.*
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.Calendar

/**
 * Manager za krunice - čuva sesije, postavke i statistike
 * ✅ FIXED: Uses Logger.error() instead of printStackTrace()
 */
class RosaryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RosaryManager"
        
        @Volatile
        private var instance: RosaryManager? = null

        fun getInstance(context: Context): RosaryManager {
            return instance ?: synchronized(this) {
                instance ?: RosaryManager(context = context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val sessionsFile = File(context.filesDir, "rosary_sessions.json")
    private val settingsFile = File(context.filesDir, "rosary_settings.json")
    private val statsFile = File(context.filesDir, "rosary_stats.json")

    private val _sessions = MutableStateFlow<List<RosarySession>>(emptyList())
    val sessions: StateFlow<List<RosarySession>> = _sessions.asStateFlow()

    private val _settings = MutableStateFlow(RosaryNotificationSettings())
    val settings: StateFlow<RosaryNotificationSettings> = _settings.asStateFlow()

    private val _stats = MutableStateFlow(RosaryStats())
    val stats: StateFlow<RosaryStats> = _stats.asStateFlow()

    init {
        loadSessions()
        loadSettings()
        loadStats()
    }

    /**
     * Započni novu sesiju molitvi krunice
     */
    suspend fun startSession(): RosarySession = withContext(ioDispatcher) {
        val mystery = RosaryMystery.forToday()
        val session = RosarySession(
            id = UUID.randomUUID().toString(),
            date = System.currentTimeMillis(),
            mysteryType = mystery.name,
            completed = false,
            startTime = System.currentTimeMillis(),
            decadesCompleted = 0
        )
        
        val sessions = _sessions.value.toMutableList()
        sessions.add(session)
        _sessions.value = sessions
        saveSessions()
        
        session
    }

    /**
     * Ažuriraj sesiju (napredak ili završetak)
     */
    suspend fun updateSession(
        sessionId: String,
        decadesCompleted: Int? = null,
        completed: Boolean? = null
    ) = withContext(ioDispatcher) {
        val sessions = _sessions.value.toMutableList()
        val index = sessions.indexOfFirst { it.id == sessionId }
        
        if (index != -1) {
            val session = sessions[index]
            val updatedSession = session.copy(
                decadesCompleted = decadesCompleted ?: session.decadesCompleted,
                completed = completed ?: session.completed,
                endTime = if (completed == true) System.currentTimeMillis() else session.endTime
            )
            sessions[index] = updatedSession
            _sessions.value = sessions
            saveSessions()
            
            // Ako je završeno, ažuriraj statistiku
            if (completed == true) {
                updateStatsAfterCompletion(session = updatedSession)
            }
        }
    }

    /**
     * Dohvati sesije za određeni dan
     */
    fun getSessionsForDate(timestamp: Long): List<RosarySession> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        return _sessions.value.filter { session ->
            calendar.timeInMillis = session.date
            calendar.get(Calendar.YEAR) == year &&
                    calendar.get(Calendar.MONTH) == month &&
                    calendar.get(Calendar.DAY_OF_MONTH) == day
        }
    }

    /**
     * Dohvati danas sesije
     */
    fun getTodaySessions(): List<RosarySession> {
        return getSessionsForDate(timestamp = System.currentTimeMillis())
    }

    /**
     * Spremanje postavki notifikacija
     */
    suspend fun saveNotificationSettings(settings: RosaryNotificationSettings) = 
        withContext(ioDispatcher) {
            _settings.value = settings
            val json = JSONObject().apply {
                put("enabled", settings.enabled)
                put("hour", settings.hour)
                put("minute", settings.minute)
                put("daysOfWeek", JSONArray(settings.daysOfWeek.toList()))
            }
            settingsFile.writeText(json.toString(2))
            // Auto-backup is handled at app level.
        }

    /**
     * Učitavanje sesija iz datoteke
     */
    private fun loadSessions() {
        if (!sessionsFile.exists()) return
        
        try {
            val jsonArray = JSONArray(sessionsFile.readText())
            val sessions = mutableListOf<RosarySession>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                sessions.add(
                    RosarySession(
                        id = obj.getString("id"),
                        date = obj.getLong("date"),
                        mysteryType = obj.getString("mysteryType"),
                        completed = obj.getBoolean("completed"),
                        startTime = obj.getLong("startTime"),
                        endTime = obj.optLong("endTime").takeIf { it != 0L },
                        decadesCompleted = obj.optInt("decadesCompleted", 0)
                    )
                )
            }
            
            _sessions.value = sessions
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // ✅ FIX: Use Logger instead of printStackTrace
            Logger.error(TAG, "Failed to load sessions", e)
        }
    }

    /**
     * Spremanje sesija u datoteku
     */
    private fun saveSessions() {
        try {
            val jsonArray = JSONArray()
            _sessions.value.forEach { session ->
                val obj = JSONObject().apply {
                    put("id", session.id)
                    put("date", session.date)
                    put("mysteryType", session.mysteryType)
                    put("completed", session.completed)
                    put("startTime", session.startTime)
                    session.endTime?.let { put("endTime", it) }
                    put("decadesCompleted", session.decadesCompleted)
                }
                jsonArray.put(obj)
            }
            
            sessionsFile.writeText(jsonArray.toString(2))
            // Auto-backup is handled at app level.
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // ✅ FIX: Use Logger instead of printStackTrace
            Logger.error(TAG, "Failed to save sessions", e)
        }
    }

    /**
     * Učitavanje postavki
     */
    private fun loadSettings() {
        if (!settingsFile.exists()) return
        
        try {
            val json = JSONObject(settingsFile.readText())
            val daysSet = mutableSetOf<Int>()
            val daysArray = json.getJSONArray("daysOfWeek")
            for (i in 0 until daysArray.length()) {
                daysSet.add(daysArray.getInt(i))
            }
            
            _settings.value = RosaryNotificationSettings(
                enabled = json.getBoolean("enabled"),
                hour = json.getInt("hour"),
                minute = json.getInt("minute"),
                daysOfWeek = daysSet
            )
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Operation failed", e)
        }
    }

    /**
     * Učitavanje statistike
     */
    private fun loadStats() {
        if (!statsFile.exists()) return
        
        try {
            val json = JSONObject(statsFile.readText())
            val sessionsPerMystery = mutableMapOf<String, Int>()
            val sessionsObj = json.optJSONObject("sessionsPerMystery")
            sessionsObj?.keys()?.forEach { key ->
                sessionsPerMystery[key] = sessionsObj.getInt(key)
            }
            
            _stats.value = RosaryStats(
                totalSessions = json.getInt("totalSessions"),
                completedSessions = json.getInt("completedSessions"),
                currentStreak = json.getInt("currentStreak"),
                longestStreak = json.getInt("longestStreak"),
                lastPrayedDate = json.optLong("lastPrayedDate").takeIf { it != 0L },
                sessionsPerMystery = sessionsPerMystery
            )
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Operation failed", e)
        }
    }

    /**
     * Ažuriranje statistike nakon završene sesije
     */
    private fun updateStatsAfterCompletion(session: RosarySession) {
        val currentStats = _stats.value
        
        // Inkrementirati brojače
        val newTotalSessions = currentStats.totalSessions + 1
        val newCompletedSessions = if (session.completed) currentStats.completedSessions + 1 
                                   else currentStats.completedSessions
        
        // Ažurirati streak
        val today = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val lastPrayedCal = currentStats.lastPrayedDate?.let {
            Calendar.getInstance().apply {
                timeInMillis = it
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        
        val newStreak = if (lastPrayedCal == null) {
            1
        } else {
            val daysDiff = ((today.timeInMillis - lastPrayedCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            when {
                daysDiff == 0 -> currentStats.currentStreak // Isti dan
                daysDiff == 1 -> currentStats.currentStreak + 1 // Uzastopni dan
                else -> 1 // Prekinut streak
            }
        }
        
        val newLongestStreak = maxOf(currentStats.longestStreak, newStreak)
        
        // Ažurirati sesije po otajstvu
        val sessionsPerMystery = currentStats.sessionsPerMystery.toMutableMap()
        sessionsPerMystery[session.mysteryType] = 
            (sessionsPerMystery[session.mysteryType] ?: 0) + 1
        
        val newStats = RosaryStats(
            totalSessions = newTotalSessions,
            completedSessions = newCompletedSessions,
            currentStreak = newStreak,
            longestStreak = newLongestStreak,
            lastPrayedDate = System.currentTimeMillis(),
            sessionsPerMystery = sessionsPerMystery
        )
        
        _stats.value = newStats
        
        // Spremiti statistiku
        try {
            val json = JSONObject().apply {
                put("totalSessions", newStats.totalSessions)
                put("completedSessions", newStats.completedSessions)
                put("currentStreak", newStats.currentStreak)
                put("longestStreak", newStats.longestStreak)
                newStats.lastPrayedDate?.let { put("lastPrayedDate", it) }
                
                val sessionsObj = JSONObject()
                newStats.sessionsPerMystery.forEach { (key, value) ->
                    sessionsObj.put(key, value)
                }
                put("sessionsPerMystery", sessionsObj)
            }
            statsFile.writeText(json.toString(2))
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Operation failed", e)
        }
    }

    /**
     * Export sesija za backup
     */
    fun exportSessions(): JSONArray {
        val jsonArray = JSONArray()
        _sessions.value.forEach { session ->
            val obj = JSONObject().apply {
                put("id", session.id)
                put("date", session.date)
                put("mysteryType", session.mysteryType)
                put("completed", session.completed)
                put("startTime", session.startTime)
                session.endTime?.let { put("endTime", it) }
                put("decadesCompleted", session.decadesCompleted)
            }
            jsonArray.put(obj)
        }
        return jsonArray
    }

    /**
     * Import sesija iz backupa
     */
    suspend fun importSessions(jsonArray: JSONArray) = withContext(ioDispatcher) {
        val sessions = mutableListOf<RosarySession>()
        
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            sessions.add(
                RosarySession(
                    id = obj.getString("id"),
                    date = obj.getLong("date"),
                    mysteryType = obj.getString("mysteryType"),
                    completed = obj.getBoolean("completed"),
                    startTime = obj.getLong("startTime"),
                    endTime = obj.optLong("endTime").takeIf { it != 0L },
                    decadesCompleted = obj.optInt("decadesCompleted", 0)
                )
            )
        }
        
        _sessions.value = sessions
        saveSessions()
    }

    /**
     * Import notification settings from backup JSON
     */
    suspend fun importSettings(json: org.json.JSONObject) = withContext(ioDispatcher) {
        try {
            val daysArray = json.optJSONArray("daysOfWeek")
            val daysSet = mutableSetOf<Int>()
            if (daysArray != null) {
                for (i in 0 until daysArray.length()) daysSet.add(daysArray.getInt(i))
            }

            val settings = RosaryNotificationSettings(
                enabled = json.optBoolean("enabled", false),
                hour = json.optInt("hour", 20),
                minute = json.optInt("minute", 0),
                daysOfWeek = daysSet
            )

            saveNotificationSettings(settings = settings)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Failed to import settings", e)
        }
    }

    /**
     * Import rosary statistics from backup JSON
     */
    suspend fun importStats(json: org.json.JSONObject) = withContext(ioDispatcher) {
        try {
            val sessionsPerMystery = mutableMapOf<String, Int>()
            val sessionsObj = json.optJSONObject("sessionsPerMystery")
            sessionsObj?.keys()?.forEach { key ->
                sessionsPerMystery[key] = sessionsObj.getInt(key)
            }

            val stats = RosaryStats(
                totalSessions = json.optInt("totalSessions", 0),
                completedSessions = json.optInt("completedSessions", 0),
                currentStreak = json.optInt("currentStreak", 0),
                longestStreak = json.optInt("longestStreak", 0),
                lastPrayedDate = json.optLong("lastPrayedDate" ).takeIf { it != 0L },
                sessionsPerMystery = sessionsPerMystery
            )

            _stats.value = stats
            // Persist stats to file
            try {
                val jsonOut = org.json.JSONObject().apply {
                    put("totalSessions", stats.totalSessions)
                    put("completedSessions", stats.completedSessions)
                    put("currentStreak", stats.currentStreak)
                    put("longestStreak", stats.longestStreak)
                    stats.lastPrayedDate?.let { put("lastPrayedDate", it) }

                    val sessionsObjOut = org.json.JSONObject()
                    stats.sessionsPerMystery.forEach { (k, v) -> sessionsObjOut.put(k, v) }
                    put("sessionsPerMystery", sessionsObjOut)
                }
                statsFile.writeText(jsonOut.toString(2))
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "Failed to persist stats", e)
            }

        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Failed to import stats", e)
        }
    }
}
