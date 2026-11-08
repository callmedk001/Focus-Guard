package com.dk.focusguard.data.repository

import com.dk.focusguard.data.local.dao.BlockedAppDao
import com.dk.focusguard.data.local.dao.MonitoredAppDao
import com.dk.focusguard.data.local.entity.BlockedAppEntity
import com.dk.focusguard.data.local.entity.MonitoredAppEntity
import com.dk.focusguard.domain.repository.MonitoredAppsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class MonitoredAppsRepositoryImpl(
    private val monitoredAppDao: MonitoredAppDao,
    private val legacyBlockedAppDao: BlockedAppDao? = null
) : MonitoredAppsRepository {

    override fun getAllMonitoredApps(): Flow<List<MonitoredAppEntity>> {
        return monitoredAppDao.getAll().catch { emit(emptyList()) }
    }

    override fun getEnabledMonitoredApps(): Flow<List<MonitoredAppEntity>> {
        return monitoredAppDao.getEnabledApps().catch { emit(emptyList()) }
    }

    override fun getMonitoredAppsCount(): Flow<Int> {
        return monitoredAppDao.getCount().catch { emit(0) }
    }

    override suspend fun getMonitoredAppsCountDirect(): Int {
        return runCatching { monitoredAppDao.getCountDirect() }.getOrDefault(0)
    }

    override suspend fun addMonitoredApp(app: MonitoredAppEntity): Result<Unit> = runCatching {
        monitoredAppDao.insert(app)
        legacyBlockedAppDao?.insertOrUpdate(
            BlockedAppEntity(
                packageName = app.packageName,
                appName = app.appName,
                isBlocked = app.isEnabled,
                category = app.category,
                blockShortsOrReelsOnly = app.blockShortsOrReelsOnly,
                dailyLimitMinutes = 0,
                createdAt = app.dateAdded
            )
        )
    }

    override suspend fun updateAppEnabled(packageName: String, isEnabled: Boolean): Result<Unit> = runCatching {
        monitoredAppDao.updateEnabledStatus(packageName, isEnabled)
        legacyBlockedAppDao?.updateBlockStatus(packageName, isEnabled)
    }

    override suspend fun removeMonitoredApp(packageName: String): Result<Unit> = runCatching {
        monitoredAppDao.delete(packageName)
        legacyBlockedAppDao?.delete(packageName)
    }

    override suspend fun resetMonitoredApps(): Result<Unit> = runCatching {
        monitoredAppDao.deleteAll()
        legacyBlockedAppDao?.delete("com.google.android.youtube")
        legacyBlockedAppDao?.delete("com.instagram.android")
    }
}

