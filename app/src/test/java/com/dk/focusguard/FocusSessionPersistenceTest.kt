package com.dk.focusguard

import com.dk.focusguard.data.local.entity.FocusSessionEntity
import com.dk.focusguard.data.repository.FocusSessionRepositoryImpl
import com.dk.focusguard.fakes.FakeFocusSessionDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FocusSessionPersistenceTest {

    private lateinit var fakeDao: FakeFocusSessionDao
    private lateinit var repository: FocusSessionRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeFocusSessionDao()
        repository = FocusSessionRepositoryImpl(dao = fakeDao)
    }

    @Test
    fun recordSession_insertsCompletedSessionWithCorrectStatus() = runBlocking {
        val now = 1000000L
        val session = FocusSessionEntity(
            title = "Deep Focus",
            startTimeMillis = now,
            endTimeMillis = now + (25 * 60 * 1000L),
            plannedDurationMinutes = 25,
            actualDurationSeconds = 25 * 60,
            isCompleted = true,
            status = "COMPLETED"
        )

        val result = repository.recordSession(session)
        assertTrue(result.isSuccess)
        val id = result.getOrThrow()
        assertTrue(id > 0)

        val sessions = repository.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("COMPLETED", sessions[0].status)
        assertTrue(sessions[0].isCompleted)
        assertEquals(25 * 60L, sessions[0].actualDurationSeconds)
    }

    @Test
    fun recordSession_insertsInterruptedSession() = runBlocking {
        val now = 1000000L
        val session = FocusSessionEntity(
            title = "Quick Block",
            startTimeMillis = now,
            endTimeMillis = now + (10 * 60 * 1000L),
            plannedDurationMinutes = 30,
            actualDurationSeconds = 10 * 60,
            isCompleted = false,
            status = "INTERRUPTED"
        )

        val result = repository.recordSession(session)
        assertTrue(result.isSuccess)

        val sessions = repository.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("INTERRUPTED", sessions[0].status)
        assertFalse(sessions[0].isCompleted)
    }

    @Test
    fun getRecentSessions_returnsSessionsInDescendingTimeOrder() = runBlocking {
        val session1 = FocusSessionEntity(title = "S1", startTimeMillis = 1000L, endTimeMillis = 2000L, plannedDurationMinutes = 25, actualDurationSeconds = 1000, isCompleted = true)
        val session2 = FocusSessionEntity(title = "S2", startTimeMillis = 3000L, endTimeMillis = 4000L, plannedDurationMinutes = 25, actualDurationSeconds = 1000, isCompleted = true)
        val session3 = FocusSessionEntity(title = "S3", startTimeMillis = 2000L, endTimeMillis = 3000L, plannedDurationMinutes = 25, actualDurationSeconds = 1000, isCompleted = true)

        repository.recordSession(session1)
        repository.recordSession(session2)
        repository.recordSession(session3)

        val recent = repository.getRecentSessions(limit = 2).first()
        assertEquals(2, recent.size)
        // Most recent first: 3000L then 2000L
        assertEquals(3000L, recent[0].startTimeMillis)
        assertEquals(2000L, recent[1].startTimeMillis)
    }

    @Test
    fun getTotalSavedSessionsCount_reflectsExactTotal() = runBlocking {
        assertEquals(0, repository.getTotalSavedSessionsCount().first())
        assertEquals(0, repository.getTotalSavedSessionsCountDirect())

        repository.recordSession(FocusSessionEntity(title = "S1", startTimeMillis = 1000L, endTimeMillis = 2000L, plannedDurationMinutes = 15, actualDurationSeconds = 900, isCompleted = true))
        repository.recordSession(FocusSessionEntity(title = "S2", startTimeMillis = 3000L, endTimeMillis = 4000L, plannedDurationMinutes = 30, actualDurationSeconds = 1800, isCompleted = true))

        assertEquals(2, repository.getTotalSavedSessionsCount().first())
        assertEquals(2, repository.getTotalSavedSessionsCountDirect())
    }

    @Test
    fun deleteSession_removesSpecificSession() = runBlocking {
        val id1 = repository.recordSession(FocusSessionEntity(title = "S1", startTimeMillis = 1000L, endTimeMillis = 2000L, plannedDurationMinutes = 15, actualDurationSeconds = 900, isCompleted = true)).getOrThrow()
        val id2 = repository.recordSession(FocusSessionEntity(title = "S2", startTimeMillis = 3000L, endTimeMillis = 4000L, plannedDurationMinutes = 30, actualDurationSeconds = 1800, isCompleted = true)).getOrThrow()

        val deleteResult = repository.deleteSession(id1)
        assertTrue(deleteResult.isSuccess)

        val remaining = repository.getAllSessions().first()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining[0].id)
    }

    @Test
    fun clearAllSessions_wipesAllSessions() = runBlocking {
        repository.recordSession(FocusSessionEntity(title = "S1", startTimeMillis = 1000L, endTimeMillis = 2000L, plannedDurationMinutes = 15, actualDurationSeconds = 900, isCompleted = true))
        val clearResult = repository.clearAllSessions()
        assertTrue(clearResult.isSuccess)

        val sessions = repository.getAllSessions().first()
        assertTrue(sessions.isEmpty())
        assertEquals(0, repository.getTotalSavedSessionsCountDirect())
    }
}
