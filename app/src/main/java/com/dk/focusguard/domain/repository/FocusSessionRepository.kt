package com.dk.focusguard.domain.repository

import com.dk.focusguard.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract managing completed and in-progress deep work focus sessions.
 */
interface FocusSessionRepository {
    fun getAllSessions(): Flow<List<FocusSessionEntity>>
    fun getRecentSessions(limit: Int): Flow<List<FocusSessionEntity>>
    fun getSessionsBetween(startTime: Long, endTime: Long): Flow<List<FocusSessionEntity>>
    fun getTotalFocusDurationSince(sinceTimestamp: Long): Flow<Long>
    fun getTotalSavedSessionsCount(): Flow<Int>
    suspend fun getTotalSavedSessionsCountDirect(): Int
    suspend fun recordSession(session: FocusSessionEntity): Result<Long>
    suspend fun updateSession(session: FocusSessionEntity): Result<Unit>
    suspend fun deleteSession(id: Long): Result<Unit>
    suspend fun clearAllSessions(): Result<Unit>
}

