package com.dk.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dk.focusguard.data.local.entity.ProtectionSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectionSettingsDao {

    @Query("SELECT * FROM protection_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<ProtectionSettingsEntity?>

    @Query("SELECT * FROM protection_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): ProtectionSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: ProtectionSettingsEntity)

    @Update
    suspend fun update(settings: ProtectionSettingsEntity)

    @Query("UPDATE protection_settings SET blockYouTubeShorts = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateShortsProtection(enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE protection_settings SET blockInstagramReels = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateReelsProtection(enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE protection_settings SET blockAdultWebsites = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateAdultProtection(enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE protection_settings SET isStrictModeEnabled = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateStrictMode(enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE protection_settings SET isMasterProtectionEnabled = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateMasterProtection(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
}

