package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow

object BibleSequentialPrefs : BasePrefs() {
    private val KEY_BIBLE_SEQUENTIAL_BOOK_INDEX = intPreferencesKey("bible_sequential_book_index")
    private val KEY_BIBLE_SEQUENTIAL_CHAPTER = intPreferencesKey("bible_sequential_chapter")

    fun bibleSequentialBookIndexFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_BIBLE_SEQUENTIAL_BOOK_INDEX, 0)

    fun bibleSequentialChapterFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_BIBLE_SEQUENTIAL_CHAPTER, 1)

    suspend fun setBibleSequentialProgress(context: Context, bookIndex: Int, chapter: Int) {
        editPref(context) {
            it[KEY_BIBLE_SEQUENTIAL_BOOK_INDEX] = bookIndex
            it[KEY_BIBLE_SEQUENTIAL_CHAPTER] = chapter
        }
    }

    suspend fun resetBibleSequentialProgress(context: Context) {
        editPref(context) {
            it[KEY_BIBLE_SEQUENTIAL_BOOK_INDEX] = 0
            it[KEY_BIBLE_SEQUENTIAL_CHAPTER] = 1
        }
    }
}