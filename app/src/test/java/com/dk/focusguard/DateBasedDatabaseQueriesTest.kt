package com.dk.focusguard

import com.dk.focusguard.data.local.entity.BlockEventEntity
import com.dk.focusguard.data.local.entity.FocusSessionEntity
import com.dk.focusguard.data.repository.BlockEventRepositoryImpl
import com.dk.focusguard.data.repository.FocusSessionRepositoryImpl
import com.dk.focusguard.fakes.FakeBlockEventDao
import com.dk.focusguard.fakes.FakeFocusSessionDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DateBasedDatabaseQueriesTest {

    private lateinit var sessionDao: FakeFocusSessionDao
    private lateinit var sessionRepository: FocusSessionRepositoryImpl
    private lateinit var blockEventDao: FakeBlockEventDao
    private lateinit var blockEventRepository: BlockEventRepositoryImpl

    @Before
    fun setUp() {
        sessionDao = FakeFocusSessionDao()
        sessionRepository = FocusSessionRepositoryImpl(dao = sessionDao)
        blockEventDao = FakeBlockEventDao()
        blockEventRepository = BlockEventRepositoryImpl(dao = blockEventDao)
    }

    @Test
    fun getSessionsBetween_returnsOnlySessionsInWindow() = runBlocking {
        // Sessions at t=100, 200, 300, 400
        sessionRepository.recordSession(
            FocusSessionEntity(title = "Block 100", startTimeMillis = 100L, endTimeMillis = 150L, plannedDurationMinutes = 25, actualDurationSeconds = 1500, isCompleted = true)
        )
        sessionRepository.recordSession(
            FocusSessionEntity(title = "Block 200", startTimeMillis = 200L, endTimeMillis = 250L, plannedDurationMinutes = 25, actualDurationSeconds = 1500, isCompleted = true)
        )
        sessionRepository.recordSession(
            FocusSessionEntity(title = "Block 300", startTimeMillis = 300L, endTimeMillis = 350L, plannedDurationMinutes = 25, actualDurationSeconds = 1500, isCompleted = true)
        )
        sessionRepository.recordSession(
            FocusSessionEntity(title = "Block 400", startTimeMillis = 400L, endTimeMillis = 450L, plannedDurationMinutes = 25, actualDurationSeconds = 1500, isCompleted = true)
        )

        // Query between 150 and 350: should match 200 and 300
        val results = sessionRepository.getSessionsBetween(startTime = 150L, endTime = 350L).first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.startTimeMillis == 200L })
        assertTrue(results.any { it.startTimeMillis == 300L })
    }

    @Test
    fun getTotalFocusDurationSince_aggregatesDurationCorrectly() = runBlocking {
        // Completed session within range: 1800s
        sessionRepository.recordSession(
            FocusSessionEntity(title = "S1", startTimeMillis = 2000L, endTimeMillis = 3000L, plannedDurationMinutes = 30, actualDurationSeconds = 1800, isCompleted = true)
        )
        // Completed session within range: 1200s
        sessionRepository.recordSession(
            FocusSessionEntity(title = "S2", startTimeMillis = 2500L, endTimeMillis = 3500L, plannedDurationMinutes = 20, actualDurationSeconds = 1200, isCompleted = true)
        )
        // Incomplete/Interrupted session within range: should NOT be counted in completed duration
        sessionRepository.recordSession(
            FocusSessionEntity(title = "S3", startTimeMillis = 2600L, endTimeMillis = 2800L, plannedDurationMinutes = 20, actualDurationSeconds = 600, isCompleted = false)
        )
        // Completed session BEFORE range (startTimeMillis = 500L): should NOT be counted
        sessionRepository.recordSession(
            FocusSessionEntity(title = "S4", startTimeMillis = 500L, endTimeMillis = 1500L, plannedDurationMinutes = 25, actualDurationSeconds = 1500, isCompleted = true)
        )

        val totalDuration = sessionRepository.getTotalFocusDurationSince(sinceTimestamp = 1000L).first()
        // 1800 + 1200 = 3000
        assertEquals(3000L, totalDuration)
    }

    @Test
    fun getRealEventsBetween_filtersEventsStrictlyByTimestamp() = runBlocking {
        blockEventRepository.recordRealBlockEvent(
            BlockEventEntity(packageName = "com.google.android.youtube", category = "Shorts", timestamp = 1000L, detectionSource = "ACCESSIBILITY", eventStatus = "INTERCEPTED")
        )
        blockEventRepository.recordRealBlockEvent(
            BlockEventEntity(packageName = "com.instagram.android", category = "Reels", timestamp = 2000L, detectionSource = "ACCESSIBILITY", eventStatus = "INTERCEPTED")
        )
        blockEventRepository.recordRealBlockEvent(
            BlockEventEntity(packageName = "com.android.chrome", category = "Adult", timestamp = 3000L, detectionSource = "VPN", eventStatus = "INTERCEPTED")
        )

        // Query between 1500 and 2500 -> should only return the Instagram event
        val windowEvents = blockEventRepository.getRealEventsBetween(startTime = 1500L, endTime = 2500L).first()
        assertEquals(1, windowEvents.size)
        assertEquals("com.instagram.android", windowEvents[0].packageName)
    }

    @Test
    fun getRealEventCountSince_countsOnlyEventsSinceThreshold() = runBlocking {
        blockEventRepository.recordRealBlockEvent(
            BlockEventEntity(packageName = "com.app1", category = "Distraction", timestamp = 100L, detectionSource = "ACCESSIBILITY")
        )
        blockEventRepository.recordRealBlockEvent(
            BlockEventEntity(packageName = "com.app2", category = "Distraction", timestamp = 500L, detectionSource = "ACCESSIBILITY")
        )
        blockEventRepository.recordRealBlockEvent(
            BlockEventEntity(packageName = "com.app3", category = "Distraction", timestamp = 900L, detectionSource = "ACCESSIBILITY")
        )

        val countSince400 = blockEventRepository.getRealEventCountSince(sinceTimestamp = 400L).first()
        assertEquals(2, countSince400)

        val countSince1000 = blockEventRepository.getRealEventCountSince(sinceTimestamp = 1000L).first()
        assertEquals(0, countSince1000)
    }
}
