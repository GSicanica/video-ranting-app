package com.youtube.rating.android.feature.running.viewmodel

import com.youtube.rating.android.feature.running.domain.RunningActiveSession
import com.youtube.rating.android.feature.running.domain.RoutePoint
import com.youtube.rating.android.feature.running.domain.RunningEntry
import com.youtube.rating.android.feature.running.domain.RunningRepository
import com.youtube.rating.android.feature.running.domain.RouteTracker
import com.youtube.rating.android.feature.running.domain.RunningTrackingServiceController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunningViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addEntry_withInvalidInput_setsValidationMessage() = runTest {
        val repo = FakeRunningRepository()
        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(),
            trackingServiceController = FakeTrackingServiceController(),
        )

        vm.addEntry(distanceKm = 0.0, durationMinutes = 20)
        advanceUntilIdle()

        assertEquals("Unesi valjanu distancu i trajanje.", vm.message.value)
        assertTrue(repo.entriesFlow.value.isEmpty())
    }

    @Test
    fun addEntry_withValidInput_persistsRun() = runTest {
        val repo = FakeRunningRepository()
        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(),
            trackingServiceController = FakeTrackingServiceController(),
        )

        vm.addEntry(distanceKm = 5.0, durationMinutes = 30)
        advanceUntilIdle()

        assertEquals(1, repo.entriesFlow.value.size)
        assertEquals(5.0, repo.entriesFlow.value.first().distanceKm, 0.0)
        assertEquals("Trening spremljen.", vm.message.value)
    }

    @Test
    fun clearAllEntries_removesAllSavedRuns() = runTest {
        val repo = FakeRunningRepository().apply {
            entriesFlow.value = listOf(
                RunningEntry(
                    id = 1L,
                    distanceKm = 3.0,
                    durationMinutes = 20,
                    paceMinPerKm = 6.66,
                    createdAtMillis = 1L,
                    routePoints = listOf(RoutePoint(45.0, 16.0, 1L), RoutePoint(45.001, 16.001, 2L)),
                ),
            )
        }
        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(),
            trackingServiceController = FakeTrackingServiceController(),
        )

        vm.clearAllEntries()
        advanceUntilIdle()

        assertTrue(repo.entriesFlow.value.isEmpty())
        assertEquals("Svi treninzi su obrisani.", vm.message.value)
    }

    @Test
    fun startGpsTracking_persistsActiveSessionAndStartsService() = runTest {
        val repo = FakeRunningRepository()
        val service = FakeTrackingServiceController(startResult = true)
        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(),
            trackingServiceController = service,
        )

        vm.startGpsTracking()
        runCurrent()

        assertTrue(vm.isGpsTracking.value)
        assertEquals(1, service.startCalls)
        assertTrue(repo.activeSessionFlow.value != null)

        vm.stopGpsTracking(saveSession = false)
        advanceUntilIdle()
    }

    @Test
    fun init_withSavedSession_restoresGpsTrackingState() = runTest {
        val now = System.currentTimeMillis() - 60_000L
        val savedSession = RunningActiveSession(
            startedAtEpochMillis = now,
            distanceKm = 1.2,
            routePoints = listOf(
                RoutePoint(45.0, 16.0, 1L),
                RoutePoint(45.001, 16.001, 2L),
            ),
        )
        val repo = FakeRunningRepository().apply {
            activeSessionFlow.value = savedSession
        }
        val service = FakeTrackingServiceController(startResult = true)

        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(),
            trackingServiceController = service,
        )
        runCurrent()

        assertTrue(vm.isGpsTracking.value)
        assertEquals(1.2, vm.activeDistanceKm.value, 0.0)
        assertEquals(2, vm.activeRoutePoints.value.size)
        assertTrue(vm.activeDurationSeconds.value >= 60L)
        assertEquals("Nastavljeno GPS praćenje iz prethodne sesije.", vm.message.value)

        vm.stopGpsTracking(saveSession = false)
        advanceUntilIdle()
    }

    @Test
    fun stopGpsTracking_withoutSave_clearsActiveSession() = runTest {
        val repo = FakeRunningRepository()
        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(),
            trackingServiceController = FakeTrackingServiceController(startResult = true),
        )

        vm.startGpsTracking()
        advanceTimeBy(5_000L)
        vm.stopGpsTracking(saveSession = false)
        advanceUntilIdle()

        assertTrue(repo.activeSessionFlow.value == null)
        assertTrue(!vm.isGpsTracking.value)
    }

    @Test
    fun startGpsTracking_whenServiceFails_resetsTrackingState() = runTest {
        val repo = FakeRunningRepository()
        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(),
            trackingServiceController = FakeTrackingServiceController(startResult = false),
        )

        vm.startGpsTracking()
        runCurrent()

        assertTrue(!vm.isGpsTracking.value)
        assertEquals(0.0, vm.activeDistanceKm.value, 0.0)
        assertEquals(0L, vm.activeDurationSeconds.value)
        assertTrue(vm.activeRoutePoints.value.isEmpty())
        assertEquals("Ne mogu pokrenuti pozadinsko GPS praćenje. Provjeri dozvole i pokušaj ponovno.", vm.message.value)
        assertTrue(repo.activeSessionFlow.value == null)
    }

    @Test
    fun stopGpsTracking_withInsufficientGpsData_clearsActiveSession() = runTest {
        val repo = FakeRunningRepository()
        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(
                updates = flowOf(RoutePoint(45.0, 16.0, 1L)),
            ),
            trackingServiceController = FakeTrackingServiceController(startResult = true),
        )

        vm.startGpsTracking()
        runCurrent()
        assertTrue(repo.activeSessionFlow.value != null)

        vm.stopGpsTracking(saveSession = true)
        advanceUntilIdle()

        assertTrue(repo.activeSessionFlow.value == null)
        assertTrue(vm.activeRoutePoints.value.isEmpty())
        assertEquals("Nema dovoljno GPS podataka za spremanje treninga.", vm.message.value)
    }

    @Test
    fun gpsTracking_keepsDistancePrecisionUntilDisplayFormatting() = runTest {
        val repo = FakeRunningRepository()
        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(
                updates = flow {
                    emit(RoutePoint(45.0, 16.0, 1L))
                    emit(RoutePoint(45.00009, 16.0, 2L))
                    emit(RoutePoint(45.00018, 16.0, 3L))
                },
            ),
            trackingServiceController = FakeTrackingServiceController(startResult = true),
        )

        vm.startGpsTracking()
        runCurrent()

        assertTrue(vm.activeDistanceKm.value > 0.019)
        assertTrue(vm.activeDistanceKm.value < 0.021)

        vm.stopGpsTracking(saveSession = false)
        advanceUntilIdle()
    }

    @Test
    fun stopGpsTracking_withSave_clearsPersistedSessionAndStoresEntry() = runTest {
        val now = System.currentTimeMillis() - 61_000L
        val repo = FakeRunningRepository().apply {
            activeSessionFlow.value = RunningActiveSession(
                startedAtEpochMillis = now,
                distanceKm = 1.2,
                routePoints = listOf(
                    RoutePoint(45.0, 16.0, 1L),
                    RoutePoint(45.001, 16.001, 2L),
                ),
            )
        }
        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(),
            trackingServiceController = FakeTrackingServiceController(startResult = true),
        )

        runCurrent()
        vm.stopGpsTracking(saveSession = true)
        advanceUntilIdle()

        assertTrue(repo.activeSessionFlow.value == null)
        assertEquals(1, repo.entriesFlow.value.size)
        assertEquals("GPS trening spremljen.", vm.message.value)
    }

    @Test
    fun init_withSavedSession_whenServiceFails_clearsPersistedSession() = runTest {
        val now = System.currentTimeMillis() - 60_000L
        val savedSession = RunningActiveSession(
            startedAtEpochMillis = now,
            distanceKm = 1.2,
            routePoints = listOf(
                RoutePoint(45.0, 16.0, 1L),
                RoutePoint(45.001, 16.001, 2L),
            ),
        )
        val repo = FakeRunningRepository().apply {
            activeSessionFlow.value = savedSession
        }
        val vm = RunningViewModel(
            repository = repo,
            routeTracker = FakeRouteTracker(),
            trackingServiceController = FakeTrackingServiceController(startResult = false),
        )

        runCurrent()

        assertTrue(!vm.isGpsTracking.value)
        assertTrue(vm.activeRoutePoints.value.isEmpty())
        assertEquals(0.0, vm.activeDistanceKm.value, 0.0)
        assertEquals(0L, vm.activeDurationSeconds.value)
        assertTrue(repo.activeSessionFlow.value == null)
        assertEquals("Pronađena je aktivna GPS sesija, ali servis se nije mogao pokrenuti.", vm.message.value)
    }

    private class FakeRouteTracker(
        private val updates: Flow<RoutePoint> = emptyFlow(),
    ) : RouteTracker {
        override fun routeUpdates(): Flow<RoutePoint> = updates
    }

    private class FakeTrackingServiceController(
        private val startResult: Boolean = true,
        private val stopResult: Boolean = true,
    ) : RunningTrackingServiceController {
        var startCalls = 0
        var stopCalls = 0

        override fun start(): Boolean {
            startCalls += 1
            return startResult
        }

        override fun stop(): Boolean {
            stopCalls += 1
            return stopResult
        }
    }

    private class FakeRunningRepository : RunningRepository {
        val entriesFlow = MutableStateFlow<List<RunningEntry>>(emptyList())
        val activeSessionFlow = MutableStateFlow<RunningActiveSession?>(null)

        override val entries: Flow<List<RunningEntry>> = entriesFlow
        override val activeSession: Flow<RunningActiveSession?> = activeSessionFlow

        override suspend fun addEntry(entry: RunningEntry) {
            entriesFlow.value = listOf(entry) + entriesFlow.value
        }

        override suspend fun deleteEntry(entryId: Long) {
            entriesFlow.value = entriesFlow.value.filterNot { it.id == entryId }
        }

        override suspend fun clearEntries() {
            entriesFlow.value = emptyList()
        }

        override suspend fun setActiveSession(session: RunningActiveSession?) {
            activeSessionFlow.value = session
        }
    }
}
