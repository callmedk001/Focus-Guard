package com.dk.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dk.focusguard.data.local.entity.MonitoredAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps ORDER BY appName ASC")
    fun getAll(): Flow<List<MonitoredAppEntity>>

    @Query("SELECT * FROM monitored_apps ORDER BY appName ASC")
    suspend fun getAllDirect(): List<MonitoredAppEntity>

    @Query("SELECT * FROM monitored_apps WHERE isEnabled = 1 ORDER BY appName ASC")
    fun getEnabledApps(): Flow<List<MonitoredAppEntity>>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): MonitoredAppEntity?

    @Query("SELECT COUNT(*) FROM monitored_apps")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM monitored_apps")
    suspend fun getCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: MonitoredAppEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultApps(apps: List<MonitoredAppEntity>)

    @Update
    suspend fun update(app: MonitoredAppEntity)

    @Query("UPDATE monitored_apps SET isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun updateEnabledStatus(packageName: String, isEnabled: Boolean)

    @Query("DELETE FROM monitored_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM monitored_apps")
    suspend fun deleteAll()
}

