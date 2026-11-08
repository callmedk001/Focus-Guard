package com.dk.focusguard.ui.accessibility

import com.dk.focusguard.domain.repository.AccessibilityServiceState
import com.dk.focusguard.services.accessibility.TargetAppInfo
import com.dk.focusguard.services.accessibility.TargetAppRegistry

/**
 * UI State representation for the Accessibility Service status and onboarding screens.
 */
data class AccessibilityUiState(
    val isPermissionGranted: Boolean = false,
    val isServiceConnected: Boolean = false,
    val serviceState: AccessibilityServiceState = AccessibilityServiceState.PERMISSION_NOT_GRANTED,
    val currentForegroundPackage: String? = null,
    val currentForegroundAppName: String? = null,
    val lastEventTimestamp: Long = 0L,
    val targetApps: List<TargetAppInfo> = TargetAppRegistry.getAllTargetApps(),
    val isLoading: Boolean = false
)

