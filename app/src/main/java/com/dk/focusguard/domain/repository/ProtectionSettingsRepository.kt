package com.dk.focusguard.domain.repository

import com.dk.focusguard.data.local.entity.ProtectionSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract managing core protection switches and strict mode settings.
 */
interface ProtectionSettingsRepository {
    fun getProtectionSettings(): Flow<ProtectionSettingsEntity>
    suspend fun getProtectionSettingsDirect(): ProtectionSettingsEntity?
    suspend fun setYouTubeShortsProtection(enabled: Boolean): Result<Unit>
    suspend fun setInstagramReelsProtection(enabled: Boolean): Result<Unit>
    suspend fun setAdultWebsiteProtection(enabled: Boolean): Result<Unit>
    suspend fun setStrictMode(enabled: Boolean): Result<Unit>
    suspend fun setMasterProtection(enabled: Boolean): Result<Unit>
    suspend fun resetProtectionSettings(): Result<Unit>
}

