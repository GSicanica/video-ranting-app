package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow

object FavoritesPrefs : BasePrefs() {
    private val KEY_FAVORITE_CUSTOM_CATEGORIES = stringSetPreferencesKey("favorite_custom_categories")

    fun favoriteCustomCategoriesFlow(context: Context): Flow<Set<String>> =
        prefsFlow(context, KEY_FAVORITE_CUSTOM_CATEGORIES, emptySet())

    suspend fun setFavoriteCustomCategories(context: Context, values: Set<String>) {
        editPref(context) { it[KEY_FAVORITE_CUSTOM_CATEGORIES] = values }
    }

    suspend fun addFavoriteCustomCategory(context: Context, value: String) {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return
        editPref(context) { prefs ->
            val cur = prefs[KEY_FAVORITE_CUSTOM_CATEGORIES] ?: emptySet()
            prefs[KEY_FAVORITE_CUSTOM_CATEGORIES] = cur + trimmed
        }
    }

    suspend fun removeFavoriteCustomCategory(context: Context, value: String) {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return
        editPref(context) { prefs ->
            val cur = prefs[KEY_FAVORITE_CUSTOM_CATEGORIES] ?: emptySet()
            prefs[KEY_FAVORITE_CUSTOM_CATEGORIES] = cur - trimmed
        }
    }
}