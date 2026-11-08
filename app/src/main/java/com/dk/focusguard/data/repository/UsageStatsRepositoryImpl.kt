package com.dk.focusguard.data.repository

import com.dk.focusguard.data.local.dao.DailyUsageStatsDao
import com.dk.focusguard.data.local.entity.DailyUsageStatsEntity
import com.dk.focusguard.domain.model.DailyUsageStats
import com.dk.focusguard.domain.repository.UsageStatsRepository
import com.dk.focusguard.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UsageStatsRepositoryImpl(
    private val dailyUsageStatsDao: DailyUsageStatsDao
) : UsageStatsRepository {

    override fun getStatsForDate(date: String): Flow<List<DailyUsageStats>> {
        return dailyUsageStatsDao.getStatsForDate(date).map { list -> list.map { it.toDomain() } }
    }

    override fun getTodayStats(): Flow<List<DailyUsageStats>> {
        return getStatsForDate(DateTimeUtils.getTodayDateString())
    }

    override suspend fun recordBlockedAttempt(packageName: String, appName: String) {
        val today = DateTimeUtils.getTodayDateString()
        val existing = dailyUsageStatsDao.getStat(today, packageName)
        if (existing != null) {
            dailyUsageStatsDao.incrementBlockedAttempts(today, packageName)
        } else {
            dailyUsageStatsDao.insertOrUpdate(
                DailyUsageStatsEntity(
                    date = today,
                    packageName = packageName,
                    appName = appName,
                    usageDurationMillis = 0L,
                    openAttempts = 1,
                    blockedAttempts = 1
                )
            )
        }
    }

    override suspend fun recordUsageTime(packageName: String, appName: String, durationMillis: Long) {
        val today = DateTimeUtils.getTodayDateString()
        val existing = dailyUsageStatsDao.getStat(today, packageName)
        if (existing != null) {
            dailyUsageStatsDao.insertOrUpdate(
                existing.copy(
                    usageDurationMillis = existing.usageDurationMillis + durationMillis,
                    openAttempts = existing.openAttempts + 1
                )
            )
        } else {
            dailyUsageStatsDao.insertOrUpdate(
                DailyUsageStatsEntity(
                    date = today,
                    packageName = packageName,
                    appName = appName,
                    usageDurationMillis = durationMillis,
                    openAttempts = 1,
                    blockedAttempts = 0
                )
            )
        }
    }
}

