package com.dk.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dk.focusguard.domain.model.AppCategory
import com.dk.focusguard.domain.model.BlockedApp

/**
 * Room entity representing an application configured for monitoring and defense.
 */
@Entity(
    tableName = "monitored_apps",
    indices = [
        Index(value = ["dateAdded"]),
        Index(value = ["isEnabled"])
    ]
)
data class MonitoredAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val dateAdded: Long = System.currentTimeMillis(),
    val category: String = "SOCIAL",
    val blockShortsOrReelsOnly: Boolean = false
) {
    fun toBlockedApp(): BlockedApp {
        return BlockedApp(
            packageName = packageName,
            appName = appName,
            isBlocked = isEnabled,
            category = runCatching { AppCategory.valueOf(category) }.getOrDefault(AppCategory.OTHER),
            blockShortsOrReelsOnly = blockShortsOrReelsOnly,
            dailyLimitMinutes = 0,
            createdAt = dateAdded
        )
    }

    companion object {
        fun fromBlockedApp(app: BlockedApp): MonitoredAppEntity {
            return MonitoredAppEntity(
                packageName = app.packageName,
                appName = app.appName,
                isEnabled = app.isBlocked,
                dateAdded = app.createdAt,
                category = app.category.name,
                blockShortsOrReelsOnly = app.blockShortsOrReelsOnly
            )
        }
    }
}

