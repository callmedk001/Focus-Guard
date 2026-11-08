package com.dk.focusguard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dk.focusguard.data.local.entity.MonitoredAppEntity
import com.dk.focusguard.domain.manager.UnifiedProtectionManager
import com.dk.focusguard.domain.model.OverallProtectionStatus
import com.dk.focusguard.domain.model.ProtectionModuleState
import com.dk.focusguard.domain.model.UserSettings
import com.dk.focusguard.domain.repository.BlockEventRepository
import com.dk.focusguard.domain.repository.FocusSessionRepository
import com.dk.focusguard.domain.repository.MonitoredAppsRepository
import com.dk.focusguard.domain.repository.ProtectionSettingsRepository
import com.dk.focusguard.domain.repository.ProtectionStateRepository
import com.dk.focusguard.domain.repository.UserSettingsRepository
import com.dk.focusguard.utils.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

private data class DashboardMetrics(
    val monitoredApps: List<MonitoredAppEntity>,
    val realBlockCount: Int,
    val todayFocusSeconds: Long,
    val userSettings: UserSettings
)

class DashboardViewModel(
    private val protectionSettingsRepository: ProtectionSettingsRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository,
    private val blockEventRepository: BlockEventRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val protectionStateRepository: ProtectionStateRepository? = null,
    private val unifiedProtectionManager: UnifiedProtectionManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val todayStartMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        viewModelScope.launch {
            val metricsFlow = combine(
                monitoredAppsRepository.getEnabledMonitoredApps(),
                blockEventRepository.getRealEventCountSince(todayStartMillis),
                focusSessionRepository.getTotalFocusDurationSince(todayStartMillis),
                userSettingsRepository.getUserSettings()
            ) { monitoredApps, realBlockCount, todayFocusSeconds, userSettings ->
                DashboardMetrics(monitoredApps, realBlockCount, todayFocusSeconds, userSettings)
            }

            if (protectionStateRepository != null) {
                val protectionFlow = combine(
                    protectionSettingsRepository.getProtectionSettings(),
                    protectionStateRepository.protectionState
                ) { settings, unifiedState ->
                    settings to unifiedState
                }

                combine(protectionFlow, metricsFlow) { (protectionSettings, unifiedState), metrics ->
                    val transitionFormatted = unifiedState.nextScheduleTransitionMillis?.let { millis ->
                        DateTimeUtils.formatTimestamp(millis, "HH:mm")
                    }

                    DashboardUiState(
                        overallStatus = unifiedState.overallStatus,
                        isProtectionActive = unifiedState.isGuardingActive,
                        isStrictModeActive = protectionSettings.isStrictModeEnabled,
                        blockYouTubeShorts = protectionSettings.blockYouTubeShorts,
                        blockInstagramReels = protectionSettings.blockInstagramReels,
                        blockAdultWebsites = protectionSettings.blockAdultWebsites,
                        shortsModuleState = unifiedState.shortsState,
                        reelsModuleState = unifiedState.reelsState,
                        adultModuleState = unifiedState.adultWebsitesState,
                        activeScheduleName = unifiedState.activeScheduleName,
                        isScheduleActive = unifiedState.isScheduleActive,
                        nextScheduleTransitionFormatted = transitionFormatted,
                        isEmergencyOverrideActive = unifiedState.isEmergencyOverrideActive,
                        overrideRemainingSeconds = unifiedState.overrideRemainingSeconds,
                        distractionsBlockedToday = metrics.realBlockCount,
                        focusMinutesAchievedToday = (metrics.todayFocusSeconds / 60).toInt(),
                        dailyScreenTimeMinutes = 195,
                        dailyGoalMinutes = metrics.userSettings.dailyFocusGoalMinutes,
                        activeProtectionRulesCount = unifiedState.activeModulesCount,
                        blockedAppsCount = metrics.monitoredApps.size,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } else {
                // Fallback for legacy constructors
                combine(
                    protectionSettingsRepository.getProtectionSettings(),
                    metricsFlow
                ) { protectionSettings, metrics ->
                    var activeRulesCount = 0
                    if (protectionSettings.blockYouTubeShorts) activeRulesCount++
                    if (protectionSettings.blockInstagramReels) activeRulesCount++
                    if (protectionSettings.blockAdultWebsites) activeRulesCount++

                    val overallStatus = if (protectionSettings.isMasterProtectionEnabled) {
                        OverallProtectionStatus.ACTIVE
                    } else {
                        OverallProtectionStatus.INACTIVE
                    }

                    val moduleState = if (protectionSettings.isMasterProtectionEnabled) {
                        ProtectionModuleState.ACTIVE
                    } else {
                        ProtectionModuleState.INACTIVE
                    }

                    DashboardUiState(
                        overallStatus = overallStatus,
                        isProtectionActive = protectionSettings.isMasterProtectionEnabled,
                        isStrictModeActive = protectionSettings.isStrictModeEnabled,
                        blockYouTubeShorts = protectionSettings.blockYouTubeShorts,
                        blockInstagramReels = protectionSettings.blockInstagramReels,
                        blockAdultWebsites = protectionSettings.blockAdultWebsites,
                        shortsModuleState = if (protectionSettings.blockYouTubeShorts) moduleState else ProtectionModuleState.INACTIVE,
                        reelsModuleState = if (protectionSettings.blockInstagramReels) moduleState else ProtectionModuleState.INACTIVE,
                        adultModuleState = if (protectionSettings.blockAdultWebsites) moduleState else ProtectionModuleState.INACTIVE,
                        distractionsBlockedToday = metrics.realBlockCount,
                        focusMinutesAchievedToday = (metrics.todayFocusSeconds / 60).toInt(),
                        dailyScreenTimeMinutes = 195,
                        dailyGoalMinutes = metrics.userSettings.dailyFocusGoalMinutes,
                        activeProtectionRulesCount = activeRulesCount,
                        blockedAppsCount = metrics.monitoredApps.size,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            }
        }
    }

    fun toggleMasterProtection() {
        viewModelScope.launch {
            if (unifiedProtectionManager != null) {
                unifiedProtectionManager.toggleMasterProtection()
            } else {
                val nextState = !_uiState.value.isProtectionActive
                protectionSettingsRepository.setMasterProtection(nextState)
            }
        }
    }

    fun toggleYouTubeShorts(enabled: Boolean) {
        viewModelScope.launch {
            protectionSettingsRepository.setYouTubeShortsProtection(enabled)
            unifiedProtectionManager?.evaluateAndEnforce()
        }
    }

    fun toggleInstagramReels(enabled: Boolean) {
        viewModelScope.launch {
            protectionSettingsRepository.setInstagramReelsProtection(enabled)
            unifiedProtectionManager?.evaluateAndEnforce()
        }
    }

    fun toggleAdultWebsites(enabled: Boolean) {
        viewModelScope.launch {
            protectionSettingsRepository.setAdultWebsiteProtection(enabled)
            unifiedProtectionManager?.evaluateAndEnforce()
        }
    }

    companion object {
        fun provideFactory(
            protectionSettingsRepository: ProtectionSettingsRepository,
            monitoredAppsRepository: MonitoredAppsRepository,
            blockEventRepository: BlockEventRepository,
            focusSessionRepository: FocusSessionRepository,
            userSettingsRepository: UserSettingsRepository,
            protectionStateRepository: ProtectionStateRepository? = null,
            unifiedProtectionManager: UnifiedProtectionManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(
                    protectionSettingsRepository,
                    monitoredAppsRepository,
                    blockEventRepository,
                    focusSessionRepository,
                    userSettingsRepository,
                    protectionStateRepository,
                    unifiedProtectionManager
                ) as T
            }
        }
    }
}
