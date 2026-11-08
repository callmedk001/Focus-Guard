package com.dk.focusguard.domain.model

/**
 * State representing individual protection shield modules.
 */
enum class ProtectionModuleState {
    /**
     * Module is actively running, guarding against distractions.
     */
    ACTIVE,

    /**
     * Module is inactive (disabled by user toggle or master protection is off).
     */
    INACTIVE,

    /**
     * Module is actively enforced by an ongoing scheduled focus window.
     */
    SCHEDULED,

    /**
     * Module is intended to be active, but its background daemon/service is not running.
     */
    OFFLINE,

    /**
     * Module is intended to be active, but the required Android OS permission is missing.
     */
    PERMISSION_REQUIRED
}

/**
 * Aggregated defense posture across all security and discipline modules.
 */
enum class OverallProtectionStatus {
    /**
     * All enabled modules are active and guarding.
     */
    ACTIVE,

    /**
     * Protection is currently inactive.
     */
    INACTIVE,

    /**
     * Protection is currently governed by an active scheduled focus window.
     */
    SCHEDULED,

    /**
     * One or more enabled defense engines are stopped or offline.
     */
    OFFLINE,

    /**
     * Missing critical OS permissions to enforce protection.
     */
    PERMISSION_REQUIRED,

    /**
     * Emergency override temporarily pauses all defense modules.
     */
    OVERRIDE_ACTIVE
}

/**
 * Comprehensive snapshot of DK FOCUS GUARD unified defense state.
 */
data class UnifiedProtectionState(
    val overallStatus: OverallProtectionStatus = OverallProtectionStatus.INACTIVE,
    val shortsState: ProtectionModuleState = ProtectionModuleState.INACTIVE,
    val reelsState: ProtectionModuleState = ProtectionModuleState.INACTIVE,
    val adultWebsitesState: ProtectionModuleState = ProtectionModuleState.INACTIVE,
    val isEmergencyOverrideActive: Boolean = false,
    val overrideExpirationTimestamp: Long = 0L,
    val overrideRemainingSeconds: Long = 0L,
    val activeScheduleName: String? = null,
    val isScheduleActive: Boolean = false,
    val nextScheduleTransitionMillis: Long? = null,
    val accessibilityServiceConnected: Boolean = false,
    val vpnServiceConnected: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val hasVpnPermission: Boolean = false,
    val activeModulesCount: Int = 0
) {
    val isGuardingActive: Boolean
        get() = overallStatus == OverallProtectionStatus.ACTIVE || overallStatus == OverallProtectionStatus.SCHEDULED
}

