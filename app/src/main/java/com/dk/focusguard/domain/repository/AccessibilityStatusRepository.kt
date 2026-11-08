package com.dk.focusguard.domain.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * High-level status of the Android Accessibility detection engine.
 * Never displays "Active" merely because an app toggle is enabled.
 */
enum class AccessibilityServiceState {
    PERMISSION_NOT_GRANTED,
    PERMISSION_GRANTED_DISCONNECTED,
    CONNECTED_AND_MONITORING,
    UNAVAILABLE
}

/**
 * Domain contract for monitoring the real-time health, permission,
 * and connection status of FocusGuardAccessibilityService.
 */
interface AccessibilityStatusRepository {

    val isPermissionGranted: Flow<Boolean>

    val isServiceConnected: Flow<Boolean>

    val isCurrentlyConnected: Boolean

    val serviceState: Flow<AccessibilityServiceState>

    val currentForegroundPackage: Flow<String?>

    val currentForegroundAppName: Flow<String?>

    val lastEventTimestamp: Flow<Long>

    fun checkAndRefreshPermission(context: Context): Boolean

    fun setServiceConnected(connected: Boolean)

    fun updateForegroundApp(packageName: String, appName: String, timestamp: Long)

    fun clearForegroundApp()
}

