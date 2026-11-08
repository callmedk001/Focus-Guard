package com.dk.focusguard.domain.model

/**
 * Domain model representing daily usage and distraction metrics.
 */
data class DailyUsageStats(
    val date: String,
    val packageName: String,
    val appName: String,
    val usageDurationMillis: Long,
    val openAttempts: Int,
    val blockedAttempts: Int
)

