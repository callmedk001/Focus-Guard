package com.dk.focusguard.data.repository

import com.dk.focusguard.data.local.dao.BlockEventDao
import com.dk.focusguard.data.local.entity.BlockEventEntity
import com.dk.focusguard.domain.repository.BlockEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Implementation of BlockEventRepository managing authentic protection event logs.
 */
class BlockEventRepositoryImpl(
    private val dao: BlockEventDao
) : BlockEventRepository {

    override fun getAllRealEvents(): Flow<List<BlockEventEntity>> {
        return dao.getAll().catch { emit(emptyList()) }
    }

    override fun getRecentRealEvents(limit: Int): Flow<List<BlockEventEntity>> {
        return dao.getRecent(limit).catch { emit(emptyList()) }
    }

    override fun getRealEventsBetween(startTime: Long, endTime: Long): Flow<List<BlockEventEntity>> {
        return dao.getEventsBetween(startTime, endTime).catch { emit(emptyList()) }
    }

    override fun getRealEventCountSince(sinceTimestamp: Long): Flow<Int> {
        return dao.getEventCountSince(sinceTimestamp).catch { emit(0) }
    }

    override fun getTotalRealBlockEventsCount(): Flow<Int> {
        return dao.getTotalCount().catch { emit(0) }
    }

    override suspend fun getTotalRealBlockEventsCountDirect(): Int {
        return runCatching { dao.getTotalCountDirect() }.getOrDefault(0)
    }

    override suspend fun recordRealBlockEvent(event: BlockEventEntity): Result<Long> = runCatching {
        dao.insert(event)
    }

    override suspend fun clearRealEvents(): Result<Unit> = runCatching {
        dao.deleteAll()
    }
}

