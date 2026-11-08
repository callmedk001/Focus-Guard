package com.dk.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusScheduleDao {

    @Query("SELECT * FROM focus_schedules ORDER BY id ASC")
    fun getAll(): Flow<List<FocusScheduleEntity>>

    @Query("SELECT * FROM focus_schedules WHERE isEnabled = 1 ORDER BY id ASC")
    fun getEnabledSchedules(): Flow<List<FocusScheduleEntity>>

    @Query("SELECT * FROM focus_schedules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FocusScheduleEntity?

    @Query("SELECT COUNT(*) FROM focus_schedules")
    fun getCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: FocusScheduleEntity): Long

    @Update
    suspend fun update(schedule: FocusScheduleEntity)

    @Query("UPDATE focus_schedules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateEnabledStatus(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM focus_schedules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM focus_schedules")
    suspend fun deleteAll()
}

