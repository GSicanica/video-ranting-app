package com.youtube.rating.android.utils

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.youtube.rating.android.data.prefs.HomePrefs

/**
 * Manager for view preferences (grid/list view state)
 */
object ViewPreferencesManager {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val _gridView = MutableStateFlow(true)
    val gridView = _gridView.asStateFlow()

    fun init(context: Context) {
        scope.launch {
            val value = HomePrefs.isGridView(context.applicationContext)
            _gridView.value = value
        }
    }

    fun isGridView(): Boolean {
        return _gridView.value
    }

    fun setGridView(isGrid: Boolean, context: Context? = null) {
        _gridView.value = isGrid
        val ctx = context ?: return
        scope.launch {
            HomePrefs.setGridView(ctx.applicationContext, isGrid)
        }
    }
}