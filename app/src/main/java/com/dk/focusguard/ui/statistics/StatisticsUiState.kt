package com.dk.focusguard.ui.statistics

import com.dk.focusguard.domain.model.DailyUsageStats

data class WeeklyDayStat(
    val day: String,
    val screenHours: Float,
    val focusHours: Float,
    val isToday: Boolean = false
)

data class AppUsageStat(
    val appName: String,
    val packageName: String,
    val usageTimeFormatted: String,
    val usagePercentage: Float,
    val statusTag: String,
    val isThreat: Boolean
)

data class StatisticsUiState(
    val dailyScreenTimeFormatted: String = "3h 42m",
    val timeSavedFormatted: String = "1h 35m",
    val totalInterceptedToday: Int = 0,
    val totalInterceptedWeek: Int = 0,
    val totalFocusMinutesAchieved: Int = 0,
    val completedSessionsCount: Int = 0,
    val disciplineStreakDays: Int = 0,
    val weeklyChart: List<WeeklyDayStat> = emptyList(),
    val mostUsedApps: List<AppUsageStat> = emptyList(),
    val topDistractions: List<DailyUsageStats> = emptyList(),
    val isDemoData: Boolean = true,
    val realBlockEventsCount: Int = 0,
    val realSessionsCount: Int = 0,
    val isLoading: Boolean = false
)
