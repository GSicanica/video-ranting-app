package com.youtube.rating.android.feature.running.data

import com.youtube.rating.android.feature.running.domain.RunningActiveSession
import com.youtube.rating.android.feature.running.domain.RoutePoint
import com.youtube.rating.android.feature.running.domain.RunningEntry
import com.youtube.rating.android.feature.running.domain.RunningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class RunningRepositoryImpl(
    private val dataStoreHolder: RunningPrefsDataStore,
    private val json: Json,
) : RunningRepository {

    private companion object {
        const val MAX_STORED_RUNS = 300
        const val MAX_ROUTE_POINTS_PER_RUN = 5000
    }

    override val entries: Flow<List<RunningEntry>> =
        RunningPrefs.entriesJsonFlow(dataStoreHolder).map { raw ->
            if (raw.isBlank()) {
                emptyList()
            } else {
                runCatching {
                    json.decodeFromString(ListSerializer(RunningEntry.serializer()), raw)
                }.getOrElse {
                    emptyList()
                }
            }
        }

    override val activeSession: Flow<RunningActiveSession?> =
        RunningPrefs.activeSessionJsonFlow(dataStoreHolder).map { raw ->
            if (raw.isBlank()) {
                null
            } else {
                runCatching {
                    json.decodeFromString(RunningActiveSession.serializer(), raw)
                }.getOrNull()?.sanitized()
            }
        }

    override suspend fun addEntry(entry: RunningEntry) {
        val updated = listOf(entry.sanitized()) + entriesFromStorage().map { it.sanitized() }
        persist(updated)
    }

    override suspend fun deleteEntry(entryId: Long) {
        val updated = entriesFromStorage().filterNot { it.id == entryId }
        persist(updated)
    }

    override suspend fun clearEntries() {
        persist(emptyList())
    }

    override suspend fun setActiveSession(session: RunningActiveSession?) {
        val encoded = session
            ?.sanitized()
            ?.let { json.encodeToString(RunningActiveSession.serializer(), it) }
            .orEmpty()
        RunningPrefs.setActiveSessionJson(
            dataStoreHolder = dataStoreHolder,
            value = encoded,
        )
    }

    private suspend fun entriesFromStorage(): List<RunningEntry> {
        return entries
            .map { saved -> saved.sortedByDescending { it.createdAtMillis } }
            .first()
    }

    private suspend fun persist(entries: List<RunningEntry>) {
        val bounded = entries
            .map { it.sanitized() }
            .sortedByDescending { it.createdAtMillis }
            .take(MAX_STORED_RUNS)

        RunningPrefs.setEntriesJson(
            dataStoreHolder = dataStoreHolder,
            value = json.encodeToString(ListSerializer(RunningEntry.serializer()), bounded),
        )
    }

    private fun RunningEntry.sanitized(): RunningEntry {
        return copy(routePoints = routePoints.trimRoutePoints())
    }

    private fun RunningActiveSession.sanitized(): RunningActiveSession {
        return copy(routePoints = routePoints.trimRoutePoints())
    }

    private fun List<RoutePoint>.trimRoutePoints(): List<RoutePoint> {
        if (size <= MAX_ROUTE_POINTS_PER_RUN) return this
        val step = (size.toDouble() / MAX_ROUTE_POINTS_PER_RUN).toInt().coerceAtLeast(1)
        val reduced = filterIndexed { index, _ -> index % step == 0 }.toMutableList()
        val last = last()
        if (reduced.lastOrNull() != last) {
            reduced.add(last)
        }
        return reduced.takeLast(MAX_ROUTE_POINTS_PER_RUN)
    }
}

