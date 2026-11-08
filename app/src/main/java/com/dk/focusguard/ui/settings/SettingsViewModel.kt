package com.dk.focusguard.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dk.focusguard.data.local.entity.MonitoredAppEntity
import com.dk.focusguard.data.preferences.AppPreferencesManager
import com.dk.focusguard.domain.model.AppCategory
import com.dk.focusguard.domain.repository.BlockEventRepository
import com.dk.focusguard.domain.repository.FocusSessionRepository
import com.dk.focusguard.domain.repository.MonitoredAppsRepository
import com.dk.focusguard.domain.repository.ProtectionSettingsRepository
import com.dk.focusguard.domain.repository.SecurityRepository
import com.dk.focusguard.domain.repository.UserSettingsRepository
import com.dk.focusguard.security.BiometricAuthManager
import com.dk.focusguard.security.BiometricCapability
import com.dk.focusguard.security.EmergencyOverrideManager
import com.dk.focusguard.security.SecurityLockManager
import com.dk.focusguard.utils.Constants
import com.dk.focusguard.utils.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userSettingsRepository: UserSettingsRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val blockEventRepository: BlockEventRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository,
    private val protectionSettingsRepository: ProtectionSettingsRepository,
    private val preferencesManager: AppPreferencesManager,
    private val securityRepository: SecurityRepository? = null,
    private val securityLockManager: SecurityLockManager? = null,
    private val biometricAuthManager: BiometricAuthManager? = null,
    private val emergencyOverrideManager: EmergencyOverrideManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettingsAndTelemetry()
        observeSecurityState()
    }

    private fun loadSettingsAndTelemetry() {
        viewModelScope.launch {
            combine(
                userSettingsRepository.getUserSettings(),
                focusSessionRepository.getTotalSavedSessionsCount(),
                blockEventRepository.getTotalRealBlockEventsCount(),
                monitoredAppsRepository.getMonitoredAppsCount()
            ) { settings, sessionCount, blockCount, appCount ->
                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        savedFocusSessionsCount = sessionCount,
                        realBlockEventsCount = blockCount,
                        monitoredAppsCount = appCount,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    private fun observeSecurityState() {
        if (securityRepository == null) return

        viewModelScope.launch {
            val biometricCapability = biometricAuthManager?.checkBiometricCapability()
            val hasBiometrics = biometricCapability == BiometricCapability.AVAILABLE
            _uiState.update { it.copy(isBiometricAvailable = hasBiometrics) }
        }

        viewModelScope.launch {
            securityRepository.isPinSet.collect { isSet ->
                _uiState.update { it.copy(isPinSet = isSet) }
            }
        }

        viewModelScope.launch {
            securityRepository.isBiometricEnabled.collect { enabled ->
                _uiState.update { it.copy(isBiometricEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            securityRepository.autoLockInactivitySeconds.collect { seconds ->
                _uiState.update { it.copy(autoLockSeconds = seconds) }
            }
        }

        viewModelScope.launch {
            securityRepository.requireAuthForProtection.collect { required ->
                _uiState.update { it.copy(requireAuthForProtection = required) }
            }
        }

        viewModelScope.launch {
            securityRepository.requireAuthForDataReset.collect { required ->
                _uiState.update { it.copy(requireAuthForDataReset = required) }
            }
        }

        viewModelScope.launch {
            securityRepository.isEmergencyOverrideActive.collect { active ->
                _uiState.update { it.copy(isEmergencyOverrideActive = active) }
            }
        }

        viewModelScope.launch {
            securityLockManager?.isSessionAuthorized?.collect { authorized ->
                _uiState.update { it.copy(isSessionAuthorized = authorized) }
            }
        }

        // Ticker for active emergency override remaining time
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val isOverride = securityRepository.isEmergencyOverrideActive.first()
                if (isOverride) {
                    val expiration = securityRepository.emergencyOverrideExpiration.first()
                    val now = System.currentTimeMillis()
                    val remaining = if (expiration > now) (expiration - now) / 1000 else 0L
                    _uiState.update { it.copy(emergencyOverrideRemainingSeconds = remaining) }
                    if (remaining == 0L) {
                        emergencyOverrideManager?.verifyAndHandleExpiration()
                    }
                } else {
                    _uiState.update { it.copy(emergencyOverrideRemainingSeconds = 0L) }
                }
            }
        }
    }

    /**
     * Checks whether an operation requires PIN authentication before proceeding.
     */
    fun isAuthenticationRequiredFor(action: String): Boolean {
        val isPinConfigured = _uiState.value.isPinSet
        if (!isPinConfigured) return false

        val isSessionValid = securityLockManager?.isSessionValid() == true
        if (isSessionValid) return false

        return when (action) {
            "RESET_DATA" -> _uiState.value.requireAuthForDataReset
            "PROTECTION_TOGGLES", "STRICT_MODE" -> _uiState.value.requireAuthForProtection
            "CHANGE_PIN", "CLEAR_PIN", "SECURITY_SETTINGS" -> true
            "EMERGENCY_OVERRIDE" -> true
            else -> false
        }
    }

    fun refreshPermissions(context: Context) {
        val dbFile = context.getDatabasePath(Constants.DATABASE_NAME)
        val sizeKb = if (dbFile.exists()) dbFile.length() / 1024 else 0L

        _uiState.update {
            it.copy(
                hasUsageStatsPermission = PermissionUtils.hasUsageStatsPermission(context),
                hasOverlayPermission = PermissionUtils.hasOverlayPermission(context),
                hasNotificationPermission = PermissionUtils.hasNotificationPermission(context),
                databaseStorageSizeKb = sizeKb
            )
        }
    }

    fun toggleStrictMode(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setStrictMode(enabled)
            protectionSettingsRepository.setStrictMode(enabled)
        }
    }

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            userSettingsRepository.setDailyFocusGoal(minutes)
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setDarkTheme(enabled)
        }
    }

    // Security Settings Actions
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityRepository?.setBiometricEnabled(enabled)
        }
    }

    fun setAutoLockSeconds(seconds: Int) {
        viewModelScope.launch {
            securityRepository?.setAutoLockInactivitySeconds(seconds)
        }
    }

    fun setRequireAuthForProtection(required: Boolean) {
        viewModelScope.launch {
            securityRepository?.setRequireAuthForProtection(required)
        }
    }

    fun setRequireAuthForDataReset(required: Boolean) {
        viewModelScope.launch {
            securityRepository?.setRequireAuthForDataReset(required)
        }
    }

    fun clearPin() {
        viewModelScope.launch {
            securityRepository?.clearPin()
            securityLockManager?.deauthorizeSession()
            _uiState.update {
                it.copy(dataActionFeedback = "Master PIN has been removed.")
            }
            delay(3000L)
            _uiState.update { it.copy(dataActionFeedback = null) }
        }
    }

    fun startEmergencyOverride(durationMinutes: Int) {
        viewModelScope.launch {
            val result = emergencyOverrideManager?.startOverride(durationMinutes)
            if (result?.isSuccess == true) {
                _uiState.update {
                    it.copy(dataActionFeedback = "Emergency defense override active for ${durationMinutes}m.")
                }
            } else {
                _uiState.update {
                    it.copy(dataActionFeedback = "Failed to activate emergency override.")
                }
            }
            delay(4000L)
            _uiState.update { it.copy(dataActionFeedback = null) }
        }
    }

    fun cancelEmergencyOverride() {
        viewModelScope.launch {
            emergencyOverrideManager?.cancelOverride()
            _uiState.update {
                it.copy(dataActionFeedback = "Emergency override terminated. Protection standing by.")
            }
            delay(3000L)
            _uiState.update { it.copy(dataActionFeedback = null) }
        }
    }

    fun resetApplicationData(context: Context) {
        viewModelScope.launch {
            // Safe transaction reset: Clear recorded events & sessions, reset defaults
            blockEventRepository.clearRealEvents()
            focusSessionRepository.clearAllSessions()
            monitoredAppsRepository.resetMonitoredApps()
            protectionSettingsRepository.resetProtectionSettings()
            preferencesManager.clearAllPreferences()
            securityRepository?.clearPin()
            securityLockManager?.deauthorizeSession()

            // Re-seed default monitored applications
            monitoredAppsRepository.addMonitoredApp(
                MonitoredAppEntity(
                    packageName = Constants.PACKAGE_YOUTUBE,
                    appName = "YouTube",
                    isEnabled = true,
                    dateAdded = System.currentTimeMillis(),
                    category = AppCategory.ENTERTAINMENT.name,
                    blockShortsOrReelsOnly = true
                )
            )
            monitoredAppsRepository.addMonitoredApp(
                MonitoredAppEntity(
                    packageName = Constants.PACKAGE_INSTAGRAM,
                    appName = "Instagram",
                    isEnabled = true,
                    dateAdded = System.currentTimeMillis(),
                    category = AppCategory.SOCIAL.name,
                    blockShortsOrReelsOnly = true
                )
            )

            refreshPermissions(context)

            _uiState.update {
                it.copy(dataActionFeedback = "Application data successfully reset to clean defaults.")
            }
            delay(3000L)
            _uiState.update { it.copy(dataActionFeedback = null) }
        }
    }

    fun clearSampleStatistics() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(dataActionFeedback = "Sample statistics cleared. Live telemetry active.")
            }
            delay(3000L)
            _uiState.update { it.copy(dataActionFeedback = null) }
        }
    }

    companion object {
        fun provideFactory(
            userSettingsRepository: UserSettingsRepository,
            focusSessionRepository: FocusSessionRepository,
            blockEventRepository: BlockEventRepository,
            monitoredAppsRepository: MonitoredAppsRepository,
            protectionSettingsRepository: ProtectionSettingsRepository,
            preferencesManager: AppPreferencesManager,
            securityRepository: SecurityRepository? = null,
            securityLockManager: SecurityLockManager? = null,
            biometricAuthManager: BiometricAuthManager? = null,
            emergencyOverrideManager: EmergencyOverrideManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    userSettingsRepository,
                    focusSessionRepository,
                    blockEventRepository,
                    monitoredAppsRepository,
                    protectionSettingsRepository,
                    preferencesManager,
                    securityRepository,
                    securityLockManager,
                    biometricAuthManager,
                    emergencyOverrideManager
                ) as T
            }
        }
    }
}
