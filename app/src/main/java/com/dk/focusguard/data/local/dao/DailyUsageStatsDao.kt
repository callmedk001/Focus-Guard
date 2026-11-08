package com.dk.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dk.focusguard.data.local.entity.DailyUsageStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageStatsDao {

    @Query("SELECT * FROM daily_usage_stats WHERE date = :date ORDER BY blockedAttempts DESC, usageDurationMillis DESC")
    fun getStatsForDate(date: String): Flow<List<DailyUsageStatsEntity>>

    @Query("SELECT * FROM daily_usage_stats WHERE date = :date AND packageName = :packageName LIMIT 1")
    suspend fun getStat(date: String, packageName: String): DailyUsageStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: DailyUsageStatsEntity)

    @Query("UPDATE daily_usage_stats SET blockedAttempts = blockedAttempts + 1 WHERE date = :date AND packageName = :packageName")
    suspend fun incrementBlockedAttempts(date: String, packageName: String): Int
}

