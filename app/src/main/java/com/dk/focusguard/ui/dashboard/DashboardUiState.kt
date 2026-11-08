package com.dk.focusguard.ui.dashboard

import com.dk.focusguard.domain.model.OverallProtectionStatus
import com.dk.focusguard.domain.model.ProtectionModuleState

/**
 * State representing the Cybersecurity Dashboard screen metrics and controls.
 * Authentically reflects live database, focus schedules, overrides, and module daemons.
 */
data class DashboardUiState(
    val overallStatus: OverallProtectionStatus = OverallProtectionStatus.INACTIVE,
    val isProtectionActive: Boolean = false,
    val isStrictModeActive: Boolean = false,
    val blockYouTubeShorts: Boolean = true,
    val blockInstagramReels: Boolean = true,
    val blockAdultWebsites: Boolean = true,
    val shortsModuleState: ProtectionModuleState = ProtectionModuleState.INACTIVE,
    val reelsModuleState: ProtectionModuleState = ProtectionModuleState.INACTIVE,
    val adultModuleState: ProtectionModuleState = ProtectionModuleState.INACTIVE,
    val activeScheduleName: String? = null,
    val isScheduleActive: Boolean = false,
    val nextScheduleTransitionFormatted: String? = null,
    val isEmergencyOverrideActive: Boolean = false,
    val overrideRemainingSeconds: Long = 0L,
    val distractionsBlockedToday: Int = 0,
    val focusMinutesAchievedToday: Int = 0,
    val dailyScreenTimeMinutes: Int = 0,
    val dailyGoalMinutes: Int = 120,
    val activeProtectionRulesCount: Int = 0,
    val blockedAppsCount: Int = 0,
    val isLoading: Boolean = false
)
