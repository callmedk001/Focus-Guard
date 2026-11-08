package com.dk.focusguard.domain.repository

import com.dk.focusguard.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing focus sessions and historical records.
 */
interface FocusRepository {
    fun getAllSessions(): Flow<List<FocusSession>>
    fun getRecentSessions(limit: Int): Flow<List<FocusSession>>
    suspend fun createSession(session: FocusSession): Long
    suspend fun updateSession(session: FocusSession)
    suspend fun getSessionById(sessionId: Long): FocusSession?
}

