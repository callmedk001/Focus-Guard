package com.dk.focusguard.data.local.entity

import androidx.room.Entity
import com.dk.focusguard.domain.model.DailyUsageStats

/**
 * Room entity representing tracked daily usage and interception count.
 */
@Entity(
    tableName = "daily_usage_stats",
    primaryKeys = ["date", "packageName"]
)
data class DailyUsageStatsEntity(
    val date: String,
    val packageName: String,
    val appName: String,
    val usageDurationMillis: Long,
    val openAttempts: Int,
    val blockedAttempts: Int
) {
    fun toDomain(): DailyUsageStats {
        return DailyUsageStats(
            date = date,
            packageName = packageName,
            appName = appName,
            usageDurationMillis = usageDurationMillis,
            openAttempts = openAttempts,
            blockedAttempts = blockedAttempts
        )
    }

    companion object {
        fun fromDomain(domain: DailyUsageStats): DailyUsageStatsEntity {
            return DailyUsageStatsEntity(
                date = domain.date,
                packageName = domain.packageName,
                appName = domain.appName,
                usageDurationMillis = domain.usageDurationMillis,
                openAttempts = domain.openAttempts,
                blockedAttempts = domain.blockedAttempts
            )
        }
    }
}

