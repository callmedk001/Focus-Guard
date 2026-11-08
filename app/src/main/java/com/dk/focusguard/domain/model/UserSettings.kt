package com.dk.focusguard.domain.model

/**
 * Domain model representing user-configurable application preferences.
 */
data class UserSettings(
    val isMasterProtectionEnabled: Boolean = true,
    val isStrictModeEnabled: Boolean = false,
    val blockYouTubeShorts: Boolean = true,
    val blockInstagramReels: Boolean = true,
    val blockAdultWebsites: Boolean = true,
    val defaultFocusDurationMinutes: Int = 25,
    val dailyFocusGoalMinutes: Int = 120,
    val darkThemeEnabled: Boolean = true,
    val isFirstLaunch: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val isPinSet: Boolean = false,
    val isFocusModeActive: Boolean = false
)
