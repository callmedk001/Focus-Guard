package com.dk.focusguard.domain.repository

import com.dk.focusguard.data.local.entity.BlockEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract managing real blocked interception telemetry events.
 *
 * CRITICAL REQUIREMENT:
 * This repository handles only genuine hardware/system-level events produced by actual
 * background defense services. Never generates or stores fake blocking records.
 */
interface BlockEventRepository {
    fun getAllRealEvents(): Flow<List<BlockEventEntity>>
    fun getRecentRealEvents(limit: Int): Flow<List<BlockEventEntity>>
    fun getRealEventsBetween(startTime: Long, endTime: Long): Flow<List<BlockEventEntity>>
    fun getRealEventCountSince(sinceTimestamp: Long): Flow<Int>
    fun getTotalRealBlockEventsCount(): Flow<Int>
    suspend fun getTotalRealBlockEventsCountDirect(): Int
    suspend fun recordRealBlockEvent(event: BlockEventEntity): Result<Long>
    suspend fun clearRealEvents(): Result<Unit>
}

