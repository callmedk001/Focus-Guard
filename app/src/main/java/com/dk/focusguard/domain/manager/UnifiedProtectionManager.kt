package com.dk.focusguard.domain.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dk.focusguard.data.local.entity.BlockEventEntity
import com.dk.focusguard.domain.model.OverallProtectionStatus
import com.dk.focusguard.domain.model.ProtectionModuleState
import com.dk.focusguard.domain.model.UnifiedProtectionState
import com.dk.focusguard.domain.repository.AccessibilityStatusRepository
import com.dk.focusguard.domain.repository.BlockEventRepository
import com.dk.focusguard.domain.repository.FocusScheduleRepository
import com.dk.focusguard.domain.repository.ProtectionSettingsRepository
import com.dk.focusguard.domain.repository.ProtectionStateRepository
import com.dk.focusguard.domain.repository.SecurityRepository
import com.dk.focusguard.domain.repository.VpnConnectionState
import com.dk.focusguard.domain.repository.VpnStatusRepository
import com.dk.focusguard.receivers.ScheduleAlarmReceiver
import com.dk.focusguard.security.EmergencyOverrideManager
import com.dk.focusguard.services.notification.ProtectionNotificationManager
import com.dk.focusguard.services.vpn.VpnConnectionManager
import com.dk.focusguard.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Central coordinator orchestrating the 3 permitted protection modules:
 * 1. YouTube Shorts Shield (Phase 6)
 * 2. Instagram Reels Guard (Phase 7)
 * 3. Adult Website Blocker via local DNS filter (Phase 8)
 *
 * Responsibilities:
 * - Coordinates schedules, emergency overrides, and manual toggles.
 * - Manages automatic start/stop of local VPN filter.
 * - Schedules AlarmManager transitions without battery-draining polling.
 * - Recovers state cleanly across system reboots and updates.
 * - Updates persistent protection status notification.
 */
class UnifiedProtectionManager(
    private val context: Context,
    private val protectionStateRepository: ProtectionStateRepository,
    private val protectionSettingsRepository: ProtectionSettingsRepository,
    private val focusScheduleRepository: FocusScheduleRepository,
    private val securityRepository: SecurityRepository,
    private val accessibilityStatusRepository: AccessibilityStatusRepository,
    private val vpnStatusRepository: VpnStatusRepository,
    private val emergencyOverrideManager: EmergencyOverrideManager,
    private val vpnConnectionManager: VpnConnectionManager,
    private val blockEventRepository: BlockEventRepository,
    private val notificationManager: ProtectionNotificationManager,
    private val coroutineScope: CoroutineScope
) {

    companion object {
        const val REQUEST_CODE_SCHEDULE_ALARM = 4201
        const val REQUEST_CODE_OVERRIDE_ALARM = 4202
        const val ACTION_SCHEDULE_TRANSITION = "com.dk.focusguard.action.SCHEDULE_TRANSITION"
    }

    val protectionState: StateFlow<UnifiedProtectionState> = protectionStateRepository.protectionState

    private var lastScheduledTransitionMillis: Long? = null
    private var overrideTickerJob: kotlinx.coroutines.Job? = null

    init {
        startProtectionObservation()
    }

    private fun startProtectionObservation() {
        coroutineScope.launch {
            protectionState.collect { state ->
                // Update persistent notification
                notificationManager.updateNotification(state)

                // Coordinate VPN service activation/deactivation for Adult Website Protection
                handleVpnCoordination(state)

                // Schedule alarm for next focus schedule boundary
                if (state.nextScheduleTransitionMillis != lastScheduledTransitionMillis) {
                    lastScheduledTransitionMillis = state.nextScheduleTransitionMillis
                    scheduleTransitionAlarm(state.nextScheduleTransitionMillis)
                }

                // If emergency override is active, schedule an alarm for its auto-expiration
                if (state.isEmergencyOverrideActive && state.overrideExpirationTimestamp > System.currentTimeMillis()) {
                    scheduleOverrideExpirationAlarm(state.overrideExpirationTimestamp)

                    // Foreground real-time ticker for auto-expiration without waiting for OS alarm
                    if (overrideTickerJob == null || !overrideTickerJob!!.isActive) {
                        overrideTickerJob = coroutineScope.launch {
                            while (isActive) {
                                kotlinx.coroutines.delay(1000L)
                                val exp = protectionState.value.overrideExpirationTimestamp
                                val now = System.currentTimeMillis()
                                if (now >= exp && exp > 0) {
                                    emergencyOverrideManager.verifyAndHandleExpiration()
                                    protectionStateRepository.refreshProtectionState()
                                    break
                                }
                            }
                        }
                    }
                } else if (!state.isEmergencyOverrideActive) {
                    overrideTickerJob?.cancel()
                    overrideTickerJob = null
                }
            }
        }
    }

    private fun handleVpnCoordination(state: UnifiedProtectionState) {
        val shouldRunVpn = (state.adultWebsitesState == ProtectionModuleState.ACTIVE ||
                state.adultWebsitesState == ProtectionModuleState.SCHEDULED) &&
                !state.isEmergencyOverrideActive

        val isVpnConnected = vpnStatusRepository.isCurrentlyConnected

        if (shouldRunVpn && !isVpnConnected && vpnConnectionManager.isVpnPermissionGranted(context)) {
            runCatching {
                vpnConnectionManager.startVpn(context)
            }
        } else if (!shouldRunVpn && isVpnConnected) {
            runCatching {
                vpnConnectionManager.stopVpn(context)
            }
        }
    }

    /**
     * Schedules the next focus transition alarm with AlarmManager.
     */
    fun scheduleTransitionAlarm(targetMillis: Long?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_TRANSITION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SCHEDULE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (targetMillis == null || targetMillis <= System.currentTimeMillis()) {
            alarmManager.cancel(pendingIntent)
            return
        }

        try {
            if (PermissionUtils.canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
        }
    }

    /**
     * Schedules alarm specifically for emergency override auto-expiration.
     */
    private fun scheduleOverrideExpirationAlarm(expirationMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_TRANSITION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_OVERRIDE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (expirationMillis <= System.currentTimeMillis()) {
            alarmManager.cancel(pendingIntent)
            return
        }

        try {
            if (PermissionUtils.canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expirationMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expirationMillis, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, expirationMillis, pendingIntent)
        }
    }

    /**
     * Invoked when ScheduleAlarmReceiver triggers. Re-evaluates protection policy and transitions state.
     */
    suspend fun onScheduleAlarmTriggered() {
        val now = System.currentTimeMillis()

        // 1. Verify if emergency override has expired
        emergencyOverrideManager.verifyAndHandleExpiration()

        // 2. Refresh protection policy
        protectionStateRepository.refreshProtectionState()

        // 3. Log schedule enforcement event
        blockEventRepository.recordRealBlockEvent(
            BlockEventEntity(
                packageName = "com.dk.focusguard",
                category = "SCHEDULE_ENFORCEMENT",
                timestamp = now,
                detectionSource = "POLICY_ENGINE",
                eventStatus = "SCHEDULE_BOUNDARY_TRANSITION"
            )
        )
    }

    /**
     * Invoked by BootRecoveryReceiver upon device boot or app update.
     * Safely restores alarms, evaluates schedules, and restarts eligible guards.
     */
    suspend fun handleBootCompleted() {
        val now = System.currentTimeMillis()

        // 1. Log boot recovery event
        blockEventRepository.recordRealBlockEvent(
            BlockEventEntity(
                packageName = "com.dk.focusguard",
                category = "SYSTEM_RECOVERY",
                timestamp = now,
                detectionSource = "BOOT_RECEIVER",
                eventStatus = "DEVICE_BOOT_RECOVERY_EXECUTED"
            )
        )

        // 2. Check override expiration
        emergencyOverrideManager.verifyAndHandleExpiration()

        // 3. Refresh protection state
        protectionStateRepository.refreshProtectionState()

        val currentState = protectionStateRepository.currentProtectionState

        // 4. Update notification
        notificationManager.updateNotification(currentState)

        // 5. Schedule next alarm transition
        scheduleTransitionAlarm(currentState.nextScheduleTransitionMillis)

        // 6. Safe start of VPN if adult protection is active
        if ((currentState.adultWebsitesState == ProtectionModuleState.ACTIVE ||
                currentState.adultWebsitesState == ProtectionModuleState.SCHEDULED) &&
            !currentState.isEmergencyOverrideActive &&
            vpnConnectionManager.isVpnPermissionGranted(context)
        ) {
            vpnConnectionManager.startVpn(context)
        }
    }

    /**
     * Manually toggles master protection.
     */
    suspend fun toggleMasterProtection() {
        val currentSettings = protectionSettingsRepository.getProtectionSettingsDirect()
        val nextState = !(currentSettings?.isMasterProtectionEnabled ?: false)
        protectionSettingsRepository.setMasterProtection(nextState)
        protectionStateRepository.refreshProtectionState()
    }

    /**
     * Explicitly refreshes the entire defense posture.
     */
    suspend fun evaluateAndEnforce() {
        emergencyOverrideManager.verifyAndHandleExpiration()
        protectionStateRepository.refreshProtectionState()
    }
}
