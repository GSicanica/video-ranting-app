package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

object HomePrefs : BasePrefs() {
    private val KEY_GRID_VIEW = booleanPreferencesKey("grid_view")
    private val KEY_LOCK_FEATURED_HOME = booleanPreferencesKey("lock_featured_home")
    private val KEY_SORT_BY = stringPreferencesKey("browse_sort")
    private val KEY_QUICK_SEARCH_TERMS = stringPreferencesKey("quick_search_terms_json")

    @Volatile
    private var cachedSortBy: String? = null

    fun isGridViewFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_GRID_VIEW, true)

    suspend fun isGridView(context: Context): Boolean =
        readPref(context, KEY_GRID_VIEW, true)

    suspend fun setGridView(context: Context, isGrid: Boolean) {
        editPref(context) { it[KEY_GRID_VIEW] = isGrid }
    }

    fun lockFeaturedHomeFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_LOCK_FEATURED_HOME, true)

    suspend fun setLockFeaturedHome(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_LOCK_FEATURED_HOME] = enabled }
    }

    fun quickSearchTermsFlow(context: Context): Flow<List<String>> =
        dataStoreFlow(context = context).map { prefs ->
            parseQuickSearchTerms(raw = prefs[KEY_QUICK_SEARCH_TERMS])
        }

    suspend fun getQuickSearchTerms(context: Context): List<String> {
        return parseQuickSearchTerms(raw = readPref(context, KEY_QUICK_SEARCH_TERMS, ""))
    }

    suspend fun setQuickSearchTerms(context: Context, terms: List<String>) {
        val normalized = terms.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val json = JSONArray().apply { normalized.forEach { put(it) } }.toString()
        editPref(context) { it[KEY_QUICK_SEARCH_TERMS] = json }
    }

    suspend fun getSortBy(context: Context): String? {
        return cachedSortBy
            ?: readPrefNullable(context, KEY_SORT_BY).also { cachedSortBy = it }
    }

    suspend fun setSortBy(context: Context, sortBy: String?) {
        editPref(context) {
            if (sortBy.isNullOrBlank()) it.remove(KEY_SORT_BY) else it[KEY_SORT_BY] = sortBy
        }
        cachedSortBy = sortBy
    }

    private fun parseQuickSearchTerms(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i, "").trim()
                    if (v.isNotBlank()) add(v)
                }
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            emptyList()
        }
    }
}