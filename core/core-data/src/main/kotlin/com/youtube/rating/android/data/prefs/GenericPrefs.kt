package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object GenericPrefs : BasePrefs() {
    suspend fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return readPref(context, booleanPreferencesKey(key), defaultValue)
    }

    suspend fun setBoolean(context: Context, key: String, value: Boolean) {
        editPref(context) { it[booleanPreferencesKey(key)] = value }
    }

    suspend fun getInt(context: Context, key: String, defaultValue: Int = 0): Int {
        return readPref(context, intPreferencesKey(key), defaultValue)
    }

    suspend fun setInt(context: Context, key: String, value: Int) {
        editPref(context) { it[intPreferencesKey(key)] = value }
    }

    suspend fun getLong(context: Context, key: String, defaultValue: Long = 0L): Long {
        return readPref(context, longPreferencesKey(key), defaultValue)
    }

    suspend fun setLong(context: Context, key: String, value: Long) {
        editPref(context) { it[longPreferencesKey(key)] = value }
    }

    suspend fun getFloat(context: Context, key: String, defaultValue: Float = 0f): Float {
        return readPref(context, floatPreferencesKey(key), defaultValue)
    }

    suspend fun setFloat(context: Context, key: String, value: Float) {
        editPref(context) { it[floatPreferencesKey(key)] = value }
    }

    suspend fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return readPrefNullable(context, stringPreferencesKey(key)) ?: defaultValue
    }

    suspend fun setString(context: Context, key: String, value: String?) {
        editPref(context) {
            val prefKey = stringPreferencesKey(key)
            if (value == null) it.remove(prefKey) else it[prefKey] = value
        }
    }

    suspend fun removeKeysWithPrefix(context: Context, prefix: String) {
        editPref(context) { prefs ->
            val keys = prefs.asMap().keys
                .filter { it.name.startsWith(prefix) }
            keys.forEach { prefs.remove(it) }
        }
    }
}