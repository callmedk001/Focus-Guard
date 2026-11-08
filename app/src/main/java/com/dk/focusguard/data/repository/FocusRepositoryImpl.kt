package com.dk.focusguard.data.repository

import com.dk.focusguard.data.local.dao.FocusSessionDao
import com.dk.focusguard.data.local.entity.FocusSessionEntity
import com.dk.focusguard.domain.model.FocusSession
import com.dk.focusguard.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FocusRepositoryImpl(
    private val focusSessionDao: FocusSessionDao
) : FocusRepository {

    override fun getAllSessions(): Flow<List<FocusSession>> {
        return focusSessionDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentSessions(limit: Int): Flow<List<FocusSession>> {
        return focusSessionDao.getRecent(limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createSession(session: FocusSession): Long {
        return focusSessionDao.insert(FocusSessionEntity.fromDomain(session))
    }

    override suspend fun updateSession(session: FocusSession) {
        focusSessionDao.update(FocusSessionEntity.fromDomain(session))
    }

    override suspend fun getSessionById(sessionId: Long): FocusSession? {
        return focusSessionDao.getById(sessionId)?.toDomain()
    }
}

