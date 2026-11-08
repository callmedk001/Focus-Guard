package com.dk.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dk.focusguard.data.local.entity.BlockEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockEventDao {

    @Query("SELECT * FROM block_events ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BlockEventEntity>>

    @Query("SELECT * FROM block_events ORDER BY timestamp DESC")
    suspend fun getAllDirect(): List<BlockEventEntity>

    @Query("SELECT * FROM block_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<BlockEventEntity>>

    @Query("SELECT * FROM block_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getEventsBetween(startTime: Long, endTime: Long): Flow<List<BlockEventEntity>>

    @Query("SELECT * FROM block_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getEventsSince(sinceTimestamp: Long): Flow<List<BlockEventEntity>>

    @Query("SELECT COUNT(*) FROM block_events WHERE timestamp >= :sinceTimestamp")
    fun getEventCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_events")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_events")
    suspend fun getTotalCountDirect(): Int

    @Query("SELECT * FROM block_events WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getEventsByPackage(packageName: String): Flow<List<BlockEventEntity>>

    @Query("SELECT * FROM block_events WHERE category = :category ORDER BY timestamp DESC")
    fun getEventsByCategory(category: String): Flow<List<BlockEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: BlockEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<BlockEventEntity>)

    @Query("DELETE FROM block_events")
    suspend fun deleteAll()
}

