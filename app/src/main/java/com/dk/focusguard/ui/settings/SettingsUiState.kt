package com.dk.focusguard.ui.settings

import com.dk.focusguard.domain.model.UserSettings

/**
 * State representing user settings, permissions, database storage telemetry,
 * and security authentication status for DK FOCUS GUARD.
 */
data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val hasUsageStatsPermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,

    // Phase 3 Data Management Telemetry
    val savedFocusSessionsCount: Int = 0,
    val realBlockEventsCount: Int = 0,
    val monitoredAppsCount: Int = 0,
    val databaseStorageSizeKb: Long = 0L,
    val dataActionFeedback: String? = null,

    // Phase 4 Security Architecture
    val isPinSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val autoLockSeconds: Int = 120,
    val requireAuthForProtection: Boolean = true,
    val requireAuthForDataReset: Boolean = true,
    val isEmergencyOverrideActive: Boolean = false,
    val emergencyOverrideRemainingSeconds: Long = 0L,
    val isSessionAuthorized: Boolean = false,

    val isLoading: Boolean = false
)
