package com.dk.focusguard.data.repository

import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import com.dk.focusguard.data.local.entity.ProtectionSettingsEntity
import com.dk.focusguard.domain.model.UnifiedProtectionState
import com.dk.focusguard.domain.policy.ProtectionPolicyEngine
import com.dk.focusguard.domain.repository.AccessibilityStatusRepository
import com.dk.focusguard.domain.repository.FocusScheduleRepository
import com.dk.focusguard.domain.repository.ProtectionSettingsRepository
import com.dk.focusguard.domain.repository.ProtectionStateRepository
import com.dk.focusguard.domain.repository.SecurityRepository
import com.dk.focusguard.domain.repository.VpnConnectionState
import com.dk.focusguard.domain.repository.VpnStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

private data class PolicyCoreInputs(
    val settings: ProtectionSettingsEntity,
    val schedules: List<FocusScheduleEntity>,
    val overrideActive: Boolean,
    val overrideExpiration: Long
)

/**
 * Concrete implementation of ProtectionStateRepository aggregating real-time
 * telemetry from DAOs, services, and security preferences.
 */
class ProtectionStateRepositoryImpl(
    private val protectionSettingsRepository: ProtectionSettingsRepository,
    private val focusScheduleRepository: FocusScheduleRepository,
    private val securityRepository: SecurityRepository,
    private val accessibilityStatusRepository: AccessibilityStatusRepository,
    private val vpnStatusRepository: VpnStatusRepository,
    private val hasAccessibilityPermission: () -> Boolean = { false },
    private val hasVpnPermission: () -> Boolean = { false },
    private val coroutineScope: CoroutineScope
) : ProtectionStateRepository {

    private val _protectionState = MutableStateFlow(UnifiedProtectionState())
    override val protectionState: StateFlow<UnifiedProtectionState> = _protectionState.asStateFlow()

    override val currentProtectionState: UnifiedProtectionState
        get() = _protectionState.value

    init {
        startObserving()
    }

    private fun startObserving() {
        val coreInputsFlow = combine(
            protectionSettingsRepository.getProtectionSettings(),
            focusScheduleRepository.getEnabledSchedules(),
            securityRepository.isEmergencyOverrideActive,
            securityRepository.emergencyOverrideExpiration
        ) { settings, schedules, overrideActive, overrideExpiration ->
            PolicyCoreInputs(settings, schedules, overrideActive, overrideExpiration)
        }

        val servicesFlow = combine(
            accessibilityStatusRepository.isServiceConnected,
            vpnStatusRepository.connectionState
        ) { accessibilityConnected, vpnState ->
            accessibilityConnected to (vpnState == VpnConnectionState.VPN_CONNECTED)
        }

        coroutineScope.launch {
            combine(coreInputsFlow, servicesFlow) { core, (accessibilityConnected, vpnConnected) ->
                val accessibilityPerm = hasAccessibilityPermission()
                val vpnPerm = hasVpnPermission()

                ProtectionPolicyEngine.evaluatePolicy(
                    protectionSettings = core.settings,
                    schedules = core.schedules,
                    isEmergencyOverrideActive = core.overrideActive,
                    overrideExpirationTimestamp = core.overrideExpiration,
                    accessibilityServiceConnected = accessibilityConnected,
                    hasAccessibilityPermission = accessibilityPerm,
                    vpnServiceConnected = vpnConnected,
                    hasVpnPermission = vpnPerm,
                    calendar = Calendar.getInstance()
                )
            }.collect { newState ->
                _protectionState.value = newState
            }
        }
    }

    override suspend fun refreshProtectionState() {
        val settings = protectionSettingsRepository.getProtectionSettingsDirect() ?: ProtectionSettingsEntity()
        val schedules = focusScheduleRepository.getEnabledSchedules().first()
        val overrideActive = securityRepository.isEmergencyOverrideActive.first()
        val overrideExpiration = securityRepository.emergencyOverrideExpiration.first()
        val accessibilityConnected = accessibilityStatusRepository.isCurrentlyConnected
        val vpnConnected = vpnStatusRepository.isCurrentlyConnected
        val accessibilityPerm = hasAccessibilityPermission()
        val vpnPerm = hasVpnPermission()

        val newState = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = schedules,
            isEmergencyOverrideActive = overrideActive,
            overrideExpirationTimestamp = overrideExpiration,
            accessibilityServiceConnected = accessibilityConnected,
            hasAccessibilityPermission = accessibilityPerm,
            vpnServiceConnected = vpnConnected,
            hasVpnPermission = vpnPerm,
            calendar = Calendar.getInstance()
        )
        _protectionState.value = newState
    }
}
