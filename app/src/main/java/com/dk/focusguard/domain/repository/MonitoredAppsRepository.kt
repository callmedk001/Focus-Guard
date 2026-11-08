package com.dk.focusguard.domain.repository

import com.dk.focusguard.data.local.entity.MonitoredAppEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract managing user monitored and restricted applications.
 */
interface MonitoredAppsRepository {
    fun getAllMonitoredApps(): Flow<List<MonitoredAppEntity>>
    fun getEnabledMonitoredApps(): Flow<List<MonitoredAppEntity>>
    fun getMonitoredAppsCount(): Flow<Int>
    suspend fun getMonitoredAppsCountDirect(): Int
    suspend fun addMonitoredApp(app: MonitoredAppEntity): Result<Unit>
    suspend fun updateAppEnabled(packageName: String, isEnabled: Boolean): Result<Unit>
    suspend fun removeMonitoredApp(packageName: String): Result<Unit>
    suspend fun resetMonitoredApps(): Result<Unit>
}

