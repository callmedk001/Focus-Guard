package com.dk.focusguard.domain.model

/**
 * Domain model representing an application configured for restriction.
 */
data class BlockedApp(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true,
    val category: AppCategory = AppCategory.SOCIAL,
    val blockShortsOrReelsOnly: Boolean = false,
    val dailyLimitMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

