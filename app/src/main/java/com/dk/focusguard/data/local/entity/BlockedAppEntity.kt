package com.dk.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dk.focusguard.domain.model.AppCategory
import com.dk.focusguard.domain.model.BlockedApp

/**
 * Room entity representing an application configured for restriction.
 */
@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean,
    val category: String,
    val blockShortsOrReelsOnly: Boolean,
    val dailyLimitMinutes: Int,
    val createdAt: Long
) {
    fun toDomain(): BlockedApp {
        return BlockedApp(
            packageName = packageName,
            appName = appName,
            isBlocked = isBlocked,
            category = runCatching { AppCategory.valueOf(category) }.getOrDefault(AppCategory.OTHER),
            blockShortsOrReelsOnly = blockShortsOrReelsOnly,
            dailyLimitMinutes = dailyLimitMinutes,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(domain: BlockedApp): BlockedAppEntity {
            return BlockedAppEntity(
                packageName = domain.packageName,
                appName = domain.appName,
                isBlocked = domain.isBlocked,
                category = domain.category.name,
                blockShortsOrReelsOnly = domain.blockShortsOrReelsOnly,
                dailyLimitMinutes = domain.dailyLimitMinutes,
                createdAt = domain.createdAt
            )
        }
    }
}

