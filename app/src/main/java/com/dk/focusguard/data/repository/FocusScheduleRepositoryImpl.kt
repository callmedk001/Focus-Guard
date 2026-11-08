package com.dk.focusguard.data.repository

import com.dk.focusguard.data.local.dao.FocusScheduleDao
import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import com.dk.focusguard.domain.repository.FocusScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class FocusScheduleRepositoryImpl(
    private val dao: FocusScheduleDao
) : FocusScheduleRepository {

    override fun getAllSchedules(): Flow<List<FocusScheduleEntity>> {
        return dao.getAll().catch { emit(emptyList()) }
    }

    override fun getEnabledSchedules(): Flow<List<FocusScheduleEntity>> {
        return dao.getEnabledSchedules().catch { emit(emptyList()) }
    }

    override suspend fun saveSchedule(schedule: FocusScheduleEntity): Result<Long> = runCatching {
        dao.insert(schedule)
    }

    override suspend fun setScheduleEnabled(id: Long, isEnabled: Boolean): Result<Unit> = runCatching {
        dao.updateEnabledStatus(id, isEnabled)
    }

    override suspend fun deleteSchedule(id: Long): Result<Unit> = runCatching {
        dao.delete(id)
    }

    override suspend fun clearAllSchedules(): Result<Unit> = runCatching {
        dao.deleteAll()
    }
}

