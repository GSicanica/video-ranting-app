package com.youtube.rating.android.data.prefs

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.youtube.rating.shared.data.AppDataEntity
import com.youtube.rating.shared.data.RealmProvider
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

object AppUsagePrefs : BasePrefs() {
    private fun usageTimeKey(userToken: String?): Preferences.Key<Long> {
        val suffix = if (!userToken.isNullOrBlank()) "_$userToken" else ""
        return longPreferencesKey("app_usage_time_ms$suffix")
    }

    fun getAppUsageTimeMsFlow(context: Context, userToken: String? = null): Flow<Long> {
        return dataStoreFlow(context = context).map { prefs ->
            prefs[usageTimeKey(userToken = userToken)] ?: 0L
        }
    }

    suspend fun getAppUsageTimeMs(context: Context, userToken: String? = null): Long =
        readPref(context, usageTimeKey(userToken = userToken), 0L)

    suspend fun setAppUsageTimeMs(context: Context, timeMs: Long, userToken: String? = null) {
        editPref(context) { prefs -> prefs[usageTimeKey(userToken = userToken)] = timeMs }
    }

    suspend fun addAppUsageTimeMs(context: Context, additionalTimeMs: Long, userToken: String? = null) {
        editPref(context) { prefs ->
            val current = prefs[usageTimeKey(userToken = userToken)] ?: 0L
            prefs[usageTimeKey(userToken = userToken)] = current + additionalTimeMs
        }
        syncAppUsageTimeToRealm(context = context, userToken = userToken)
    }

    /**
     * Sync app usage time from DataStore to Realm database (per-user)
     */
    private suspend fun syncAppUsageTimeToRealm(context: Context, userToken: String? = null) {
        try {
            withContext(ioDispatcher) {
                val usageTime = getAppUsageTimeMs(context = context, userToken = userToken)
                val realm = RealmProvider.getInstance()
                val realmId = if (!userToken.isNullOrBlank()) "app_data_$userToken" else "app_data"

                realm.write {
                    val appData = query(AppDataEntity::class, "id == $0", realmId).first().find()
                    if (appData != null) {
                        appData.appUsageTimeMs = usageTime
                        appData.lastUpdated = System.currentTimeMillis()
                    } else {
                        copyToRealm(AppDataEntity().apply {
                            id = realmId
                            appUsageTimeMs = usageTime
                            lastUpdated = System.currentTimeMillis()
                        })
                    }
                }
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("AppUsagePrefs", "Failed to sync app usage time to Realm: ${e.message}", e)
        }
    }
}
