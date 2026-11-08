package com.dk.focusguard

import com.dk.focusguard.data.local.entity.BlockEventEntity
import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import com.dk.focusguard.data.local.entity.FocusSessionEntity
import com.dk.focusguard.data.local.entity.MonitoredAppEntity
import com.dk.focusguard.data.repository.BlockEventRepositoryImpl
import com.dk.focusguard.data.repository.FocusScheduleRepositoryImpl
import com.dk.focusguard.data.repository.FocusSessionRepositoryImpl
import com.dk.focusguard.data.repository.MonitoredAppsRepositoryImpl
import com.dk.focusguard.data.repository.ProtectionSettingsRepositoryImpl
import com.dk.focusguard.fakes.FakeBlockEventDao
import com.dk.focusguard.fakes.FakeFocusScheduleDao
import com.dk.focusguard.fakes.FakeFocusSessionDao
import com.dk.focusguard.fakes.FakeMonitoredAppDao
import com.dk.focusguard.fakes.FakeProtectionSettingsDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryErrorHandlingTest {

    @Test
    fun protectionSettingsRepository_catchesDaoFlowException_emitsDefaultSafely() = runBlocking {
        val dao = FakeProtectionSettingsDao()
        dao.shouldThrow = true
        val repository = ProtectionSettingsRepositoryImpl(dao = dao)

        val settings = repository.getProtectionSettings().first()
        // Should NOT crash, returns fallback default settings
        assertNotNull(settings)
        assertEquals(1, settings.id)
        assertTrue(settings.blockYouTubeShorts)
    }

    @Test
    fun protectionSettingsRepository_catchesWriteException_returnsFailureResult() = runBlocking {
        val dao = FakeProtectionSettingsDao()
        dao.shouldThrow = true
        val repository = ProtectionSettingsRepositoryImpl(dao = dao)

        val result = repository.setYouTubeShortsProtection(false)
        assertTrue(result.isFailure)
    }

    @Test
    fun monitoredAppsRepository_catchesDaoFlowException_emitsEmptyList() = runBlocking {
        val dao = FakeMonitoredAppDao()
        dao.shouldThrow = true
        val repository = MonitoredAppsRepositoryImpl(monitoredAppDao = dao)

        val apps = repository.getAllMonitoredApps().first()
        assertTrue(apps.isEmpty())

        val directCount = repository.getMonitoredAppsCountDirect()
        assertEquals(0, directCount)
    }

    @Test
    fun monitoredAppsRepository_catchesWriteException_returnsFailureResult() = runBlocking {
        val dao = FakeMonitoredAppDao()
        dao.shouldThrow = true
        val repository = MonitoredAppsRepositoryImpl(monitoredAppDao = dao)

        val app = MonitoredAppEntity("com.test", "Test App")
        val result = repository.addMonitoredApp(app)
        assertTrue(result.isFailure)
    }

    @Test
    fun focusScheduleRepository_catchesDaoException_returnsFailureAndEmptyFlow() = runBlocking {
        val dao = FakeFocusScheduleDao()
        dao.shouldThrow = true
        val repository = FocusScheduleRepositoryImpl(dao = dao)

        val schedules = repository.getAllSchedules().first()
        assertTrue(schedules.isEmpty())

        val schedule = FocusScheduleEntity(name = "Test", startTime = "10:00", endTime = "11:00", selectedWeekdays = "Mon")
        val result = repository.saveSchedule(schedule)
        assertTrue(result.isFailure)
    }

    @Test
    fun focusSessionRepository_catchesDaoException_returnsFailureAndSafeDefaults() = runBlocking {
        val dao = FakeFocusSessionDao()
        dao.shouldThrow = true
        val repository = FocusSessionRepositoryImpl(dao = dao)

        val sessions = repository.getAllSessions().first()
        assertTrue(sessions.isEmpty())

        val session = FocusSessionEntity(
            title = "Test Session",
            startTimeMillis = 1000L,
            endTimeMillis = 2000L,
            plannedDurationMinutes = 25,
            actualDurationSeconds = 1500,
            isCompleted = true
        )
        val result = repository.recordSession(session)
        assertTrue(result.isFailure)

        val count = repository.getTotalSavedSessionsCountDirect()
        assertEquals(0, count)
    }

    @Test
    fun blockEventRepository_catchesDaoException_returnsFailureAndSafeDefaults() = runBlocking {
        val dao = FakeBlockEventDao()
        dao.shouldThrow = true
        val repository = BlockEventRepositoryImpl(dao = dao)

        val events = repository.getAllRealEvents().first()
        assertTrue(events.isEmpty())

        val event = BlockEventEntity(
            packageName = "com.test",
            category = "Social",
            timestamp = 1000L,
            detectionSource = "ACCESSIBILITY"
        )
        val result = repository.recordRealBlockEvent(event)
        assertTrue(result.isFailure)

        val directCount = repository.getTotalRealBlockEventsCountDirect()
        assertEquals(0, directCount)
    }
}
