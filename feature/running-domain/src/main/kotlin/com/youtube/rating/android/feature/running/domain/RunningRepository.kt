package com.youtube.rating.android.feature.running.domain

import kotlinx.coroutines.flow.Flow

interface RunningRepository {
    val entries: Flow<List<RunningEntry>>
    val activeSession: Flow<RunningActiveSession?>

    suspend fun addEntry(entry: RunningEntry)
    suspend fun deleteEntry(entryId: Long)
    suspend fun clearEntries()
    suspend fun setActiveSession(session: RunningActiveSession?)
}
