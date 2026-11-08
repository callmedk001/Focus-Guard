package com.dk.focusguard.domain.repository

import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract managing recurring weekly focus schedules.
 */
interface FocusScheduleRepository {
    fun getAllSchedules(): Flow<List<FocusScheduleEntity>>
    fun getEnabledSchedules(): Flow<List<FocusScheduleEntity>>
    suspend fun saveSchedule(schedule: FocusScheduleEntity): Result<Long>
    suspend fun setScheduleEnabled(id: Long, isEnabled: Boolean): Result<Unit>
    suspend fun deleteSchedule(id: Long): Result<Unit>
    suspend fun clearAllSchedules(): Result<Unit>
}

