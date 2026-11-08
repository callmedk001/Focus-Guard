package com.dk.focusguard.data.repository

import com.dk.focusguard.data.local.dao.FocusSessionDao
import com.dk.focusguard.data.local.entity.FocusSessionEntity
import com.dk.focusguard.domain.repository.FocusSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class FocusSessionRepositoryImpl(
    private val dao: FocusSessionDao
) : FocusSessionRepository {

    override fun getAllSessions(): Flow<List<FocusSessionEntity>> {
        return dao.getAll().catch { emit(emptyList()) }
    }

    override fun getRecentSessions(limit: Int): Flow<List<FocusSessionEntity>> {
        return dao.getRecent(limit).catch { emit(emptyList()) }
    }

    override fun getSessionsBetween(startTime: Long, endTime: Long): Flow<List<FocusSessionEntity>> {
        return dao.getSessionsBetween(startTime, endTime).catch { emit(emptyList()) }
    }

    override fun getTotalFocusDurationSince(sinceTimestamp: Long): Flow<Long> {
        return dao.getTotalFocusDurationSecondsSince(sinceTimestamp).catch { emit(0L) }
    }

    override fun getTotalSavedSessionsCount(): Flow<Int> {
        return dao.getTotalCount().catch { emit(0) }
    }

    override suspend fun getTotalSavedSessionsCountDirect(): Int {
        return runCatching { dao.getTotalCountDirect() }.getOrDefault(0)
    }

    override suspend fun recordSession(session: FocusSessionEntity): Result<Long> = runCatching {
        dao.insert(session)
    }

    override suspend fun updateSession(session: FocusSessionEntity): Result<Unit> = runCatching {
        dao.update(session)
    }

    override suspend fun deleteSession(id: Long): Result<Unit> = runCatching {
        dao.delete(id)
    }

    override suspend fun clearAllSessions(): Result<Unit> = runCatching {
        dao.deleteAll()
    }
}

