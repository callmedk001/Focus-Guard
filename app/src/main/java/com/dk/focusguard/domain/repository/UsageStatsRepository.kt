package com.dk.focusguard.domain.repository

import com.dk.focusguard.domain.model.DailyUsageStats
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for tracking app usage and intercepted distraction attempts.
 */
interface UsageStatsRepository {
    fun getStatsForDate(date: String): Flow<List<DailyUsageStats>>
    fun getTodayStats(): Flow<List<DailyUsageStats>>
    suspend fun recordBlockedAttempt(packageName: String, appName: String)
    suspend fun recordUsageTime(packageName: String, appName: String, durationMillis: Long)
}

