package com.dk.focusguard.data.repository

import com.dk.focusguard.data.local.dao.ProtectionSettingsDao
import com.dk.focusguard.data.local.entity.ProtectionSettingsEntity
import com.dk.focusguard.data.preferences.AppPreferencesManager
import com.dk.focusguard.domain.repository.ProtectionSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ProtectionSettingsRepositoryImpl(
    private val dao: ProtectionSettingsDao,
    private val preferencesManager: AppPreferencesManager? = null
) : ProtectionSettingsRepository {

    override fun getProtectionSettings(): Flow<ProtectionSettingsEntity> {
        return dao.getSettings().map { entity ->
            entity ?: ProtectionSettingsEntity()
        }.catch {
            emit(ProtectionSettingsEntity())
        }
    }

    override suspend fun getProtectionSettingsDirect(): ProtectionSettingsEntity? {
        return runCatching { dao.getSettingsDirect() }.getOrNull()
    }

    override suspend fun setYouTubeShortsProtection(enabled: Boolean): Result<Unit> = runCatching {
        dao.updateShortsProtection(enabled)
        preferencesManager?.setBlockYouTubeShorts(enabled)
    }

    override suspend fun setInstagramReelsProtection(enabled: Boolean): Result<Unit> = runCatching {
        dao.updateReelsProtection(enabled)
        preferencesManager?.setBlockInstagramReels(enabled)
    }

    override suspend fun setAdultWebsiteProtection(enabled: Boolean): Result<Unit> = runCatching {
        dao.updateAdultProtection(enabled)
        preferencesManager?.setBlockAdultWebsites(enabled)
    }

    override suspend fun setStrictMode(enabled: Boolean): Result<Unit> = runCatching {
        dao.updateStrictMode(enabled)
        preferencesManager?.setStrictMode(enabled)
    }

    override suspend fun setMasterProtection(enabled: Boolean): Result<Unit> = runCatching {
        dao.updateMasterProtection(enabled)
        preferencesManager?.setMasterProtection(enabled)
    }

    override suspend fun resetProtectionSettings(): Result<Unit> = runCatching {
        val defaultSettings = ProtectionSettingsEntity(
            id = 1,
            blockYouTubeShorts = true,
            blockInstagramReels = true,
            blockAdultWebsites = true,
            isStrictModeEnabled = false,
            isMasterProtectionEnabled = true,
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertOrUpdate(defaultSettings)
        preferencesManager?.setBlockYouTubeShorts(true)
        preferencesManager?.setBlockInstagramReels(true)
        preferencesManager?.setBlockAdultWebsites(true)
        preferencesManager?.setStrictMode(false)
        preferencesManager?.setMasterProtection(true)
    }
}

