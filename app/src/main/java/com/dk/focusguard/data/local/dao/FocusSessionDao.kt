package com.dk.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dk.focusguard.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Query("SELECT * FROM focus_sessions ORDER BY startTimeMillis DESC")
    fun getAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startTimeMillis DESC")
    suspend fun getAllDirect(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions ORDER BY startTimeMillis DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: Long): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE startTimeMillis >= :startTime AND startTimeMillis <= :endTime ORDER BY startTimeMillis DESC")
    fun getSessionsBetween(startTime: Long, endTime: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE startTimeMillis >= :sinceTimestamp ORDER BY startTimeMillis DESC")
    fun getSessionsSince(sinceTimestamp: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT COALESCE(SUM(actualDurationSeconds), 0) FROM focus_sessions WHERE isCompleted = 1 AND startTimeMillis >= :sinceTimestamp")
    fun getTotalFocusDurationSecondsSince(sinceTimestamp: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE isCompleted = 1 AND startTimeMillis >= :sinceTimestamp")
    fun getCompletedSessionCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM focus_sessions")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM focus_sessions")
    suspend fun getTotalCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FocusSessionEntity): Long

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}
