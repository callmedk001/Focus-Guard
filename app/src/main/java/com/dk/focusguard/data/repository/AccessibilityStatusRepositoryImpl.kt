package com.dk.focusguard.data.repository

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.dk.focusguard.domain.repository.AccessibilityServiceState
import com.dk.focusguard.domain.repository.AccessibilityStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Thread-safe implementation of AccessibilityStatusRepository.
 * Manages real-time state synchronization between Android Accessibility framework
 * callbacks and presentation ViewModels.
 */
class AccessibilityStatusRepositoryImpl : AccessibilityStatusRepository {

    private val _isPermissionGranted = MutableStateFlow(false)
    override val isPermissionGranted: Flow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    override val isServiceConnected: Flow<Boolean> = _isServiceConnected.asStateFlow()
    override val isCurrentlyConnected: Boolean get() = _isServiceConnected.value

    private val _currentForegroundPackage = MutableStateFlow<String?>(null)
    override val currentForegroundPackage: Flow<String?> = _currentForegroundPackage.asStateFlow()

    private val _currentForegroundAppName = MutableStateFlow<String?>(null)
    override val currentForegroundAppName: Flow<String?> = _currentForegroundAppName.asStateFlow()

    private val _lastEventTimestamp = MutableStateFlow(0L)
    override val lastEventTimestamp: Flow<Long> = _lastEventTimestamp.asStateFlow()

    override val serviceState: Flow<AccessibilityServiceState> = combine(
        _isPermissionGranted,
        _isServiceConnected
    ) { granted, connected ->
        when {
            !granted -> AccessibilityServiceState.PERMISSION_NOT_GRANTED
            granted && !connected -> AccessibilityServiceState.PERMISSION_GRANTED_DISCONNECTED
            granted && connected -> AccessibilityServiceState.CONNECTED_AND_MONITORING
            else -> AccessibilityServiceState.UNAVAILABLE
        }
    }.distinctUntilChanged()

    override fun checkAndRefreshPermission(context: Context): Boolean {
        val granted = isAccessibilityPermissionEnabled(context)
        _isPermissionGranted.value = granted
        return granted
    }

    /**
     * Testing hook to verify state machine transitions on local JVM.
     */
    fun setPermissionGrantedForTesting(granted: Boolean) {
        _isPermissionGranted.value = granted
    }

    override fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
        if (!connected) {
            _currentForegroundPackage.value = null
            _currentForegroundAppName.value = null
        }
    }

    override fun updateForegroundApp(packageName: String, appName: String, timestamp: Long) {
        _currentForegroundPackage.value = packageName
        _currentForegroundAppName.value = appName
        _lastEventTimestamp.value = timestamp
    }

    override fun clearForegroundApp() {
        _currentForegroundPackage.value = null
        _currentForegroundAppName.value = null
    }

    companion object {
        /**
         * Inspects Android secure settings to check if FocusGuardAccessibilityService
         * is currently enabled in accessibility settings.
         */
        fun isAccessibilityPermissionEnabled(context: Context): Boolean {
            val expectedServiceComponent = "${context.packageName}/com.dk.focusguard.services.accessibility.FocusGuardAccessibilityService"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceComponent, ignoreCase = true) ||
                    componentName.contains("FocusGuardAccessibilityService", ignoreCase = true)
                ) {
                    return true
                }
            }
            return false
        }
    }
}

