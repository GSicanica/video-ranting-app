package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object GalleryPrefs : BasePrefs() {
    private val KEY_GALLERY_IMAGES = stringPreferencesKey("gallery_images")
    private val KEY_GALLERY_PINNED = stringPreferencesKey("gallery_pinned")
    private val KEY_GALLERY_DEFAULTS_LOADED = booleanPreferencesKey("gallery_defaults_loaded")

    suspend fun getGalleryImagesJson(context: Context): String =
        readPref(context, KEY_GALLERY_IMAGES, "[]")

    suspend fun getGalleryPinnedJson(context: Context): String =
        readPref(context, KEY_GALLERY_PINNED, "[]")

    suspend fun setGalleryImagesJson(context: Context, json: String) {
        editPref(context) { it[KEY_GALLERY_IMAGES] = json }
    }

    suspend fun setGalleryPinnedJson(context: Context, json: String) {
        editPref(context) { it[KEY_GALLERY_PINNED] = json }
    }

    suspend fun getGalleryDefaultsLoaded(context: Context): Boolean =
        readPref(context, KEY_GALLERY_DEFAULTS_LOADED, false)

    suspend fun setGalleryDefaultsLoaded(context: Context, loaded: Boolean) {
        editPref(context) { it[KEY_GALLERY_DEFAULTS_LOADED] = loaded }
    }
}