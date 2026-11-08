package com.dk.focusguard

import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import com.dk.focusguard.data.local.entity.ProtectionSettingsEntity
import com.dk.focusguard.domain.model.OverallProtectionStatus
import com.dk.focusguard.domain.model.ProtectionModuleState
import com.dk.focusguard.domain.policy.ProtectionPolicyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Comprehensive test suite verifying the Unified Protection Engine,
 * policy evaluation matrix, service readiness, emergency override precedence,
 * and automated focus schedule coordination.
 */
class UnifiedProtectionPolicyTest {

    private fun createMondayCalendar(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 9) // Monday
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testPolicy_masterProtectionOff_noSchedule_allInactive() {
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = false,
            blockYouTubeShorts = true,
            blockInstagramReels = true,
            blockAdultWebsites = true
        )

        val cal = createMondayCalendar(hour = 12, minute = 0)
        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = emptyList(),
            isEmergencyOverrideActive = false,
            overrideExpirationTimestamp = 0L,
            accessibilityServiceConnected = true,
            hasAccessibilityPermission = true,
            vpnServiceConnected = true,
            hasVpnPermission = true,
            calendar = cal
        )

        assertEquals(OverallProtectionStatus.INACTIVE, state.overallStatus)
        assertEquals(ProtectionModuleState.INACTIVE, state.shortsState)
        assertEquals(ProtectionModuleState.INACTIVE, state.reelsState)
        assertEquals(ProtectionModuleState.INACTIVE, state.adultWebsitesState)
        assertEquals(0, state.activeModulesCount)
        assertFalse(state.isGuardingActive)
    }

    @Test
    fun testPolicy_masterProtectionOn_allModulesOn_servicesConnected_allActive() {
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = true,
            blockYouTubeShorts = true,
            blockInstagramReels = true,
            blockAdultWebsites = true
        )

        val cal = createMondayCalendar(hour = 12, minute = 0)
        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = emptyList(),
            isEmergencyOverrideActive = false,
            overrideExpirationTimestamp = 0L,
            accessibilityServiceConnected = true,
            hasAccessibilityPermission = true,
            vpnServiceConnected = true,
            hasVpnPermission = true,
            calendar = cal
        )

        assertEquals(OverallProtectionStatus.ACTIVE, state.overallStatus)
        assertEquals(ProtectionModuleState.ACTIVE, state.shortsState)
        assertEquals(ProtectionModuleState.ACTIVE, state.reelsState)
        assertEquals(ProtectionModuleState.ACTIVE, state.adultWebsitesState)
        assertEquals(3, state.activeModulesCount)
        assertTrue(state.isGuardingActive)
    }

    @Test
    fun testPolicy_masterProtectionOn_individualModuleToggledOff() {
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = true,
            blockYouTubeShorts = false, // Shorts disabled
            blockInstagramReels = true,
            blockAdultWebsites = true
        )

        val cal = createMondayCalendar(hour = 12, minute = 0)
        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = emptyList(),
            isEmergencyOverrideActive = false,
            overrideExpirationTimestamp = 0L,
            accessibilityServiceConnected = true,
            hasAccessibilityPermission = true,
            vpnServiceConnected = true,
            hasVpnPermission = true,
            calendar = cal
        )

        assertEquals(OverallProtectionStatus.ACTIVE, state.overallStatus)
        assertEquals(ProtectionModuleState.INACTIVE, state.shortsState)
        assertEquals(ProtectionModuleState.ACTIVE, state.reelsState)
        assertEquals(ProtectionModuleState.ACTIVE, state.adultWebsitesState)
        assertEquals(2, state.activeModulesCount)
    }

    @Test
    fun testPolicy_scheduleActive_overridesMasterSwitchOff() {
        // Master switch is OFF
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = false,
            blockYouTubeShorts = false,
            blockInstagramReels = false,
            blockAdultWebsites = false
        )

        // But an active schedule is ongoing: Monday 09:00 - 17:00
        val schedule = FocusScheduleEntity(
            id = 1,
            name = "Deep Work Schedule",
            startTime = "09:00",
            endTime = "17:00",
            selectedWeekdays = "Mon,Tue,Wed,Thu,Fri",
            isEnabled = true,
            selectedAppPackages = "" // Defaults to all 3 modules
        )

        val monday10am = createMondayCalendar(hour = 10, minute = 0)
        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = listOf(schedule),
            isEmergencyOverrideActive = false,
            overrideExpirationTimestamp = 0L,
            accessibilityServiceConnected = true,
            hasAccessibilityPermission = true,
            vpnServiceConnected = true,
            hasVpnPermission = true,
            calendar = monday10am
        )

        assertEquals(OverallProtectionStatus.SCHEDULED, state.overallStatus)
        assertEquals(ProtectionModuleState.SCHEDULED, state.shortsState)
        assertEquals(ProtectionModuleState.SCHEDULED, state.reelsState)
        assertEquals(ProtectionModuleState.SCHEDULED, state.adultWebsitesState)
        assertEquals("Deep Work Schedule", state.activeScheduleName)
        assertTrue(state.isScheduleActive)
        assertEquals(3, state.activeModulesCount)
        assertTrue(state.isGuardingActive)
    }

    @Test
    fun testPolicy_scheduleWindowEnded_restoresManualSettings() {
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = false,
            blockYouTubeShorts = false,
            blockInstagramReels = false,
            blockAdultWebsites = false
        )

        val schedule = FocusScheduleEntity(
            id = 1,
            name = "Deep Work Schedule",
            startTime = "09:00",
            endTime = "17:00",
            selectedWeekdays = "Mon,Tue,Wed,Thu,Fri",
            isEnabled = true
        )

        // Monday 18:00 (schedule has ended)
        val monday18pm = createMondayCalendar(hour = 18, minute = 0)
        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = listOf(schedule),
            isEmergencyOverrideActive = false,
            overrideExpirationTimestamp = 0L,
            accessibilityServiceConnected = true,
            hasAccessibilityPermission = true,
            vpnServiceConnected = true,
            hasVpnPermission = true,
            calendar = monday18pm
        )

        // Gracefully restored manual settings: all INACTIVE
        assertEquals(OverallProtectionStatus.INACTIVE, state.overallStatus)
        assertEquals(ProtectionModuleState.INACTIVE, state.shortsState)
        assertEquals(ProtectionModuleState.INACTIVE, state.reelsState)
        assertEquals(ProtectionModuleState.INACTIVE, state.adultWebsitesState)
        assertFalse(state.isScheduleActive)
        assertEquals(0, state.activeModulesCount)
    }

    @Test
    fun testPolicy_emergencyOverride_pausesAllGuards() {
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = true,
            blockYouTubeShorts = true,
            blockInstagramReels = true,
            blockAdultWebsites = true
        )

        val cal = createMondayCalendar(hour = 12, minute = 0)
        val now = cal.timeInMillis
        val expiration = now + (15 * 60 * 1000L) // 15 mins in future

        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = emptyList(),
            isEmergencyOverrideActive = true,
            overrideExpirationTimestamp = expiration,
            accessibilityServiceConnected = true,
            hasAccessibilityPermission = true,
            vpnServiceConnected = true,
            hasVpnPermission = true,
            calendar = cal
        )

        assertEquals(OverallProtectionStatus.OVERRIDE_ACTIVE, state.overallStatus)
        assertEquals(ProtectionModuleState.INACTIVE, state.shortsState)
        assertEquals(ProtectionModuleState.INACTIVE, state.reelsState)
        assertEquals(ProtectionModuleState.INACTIVE, state.adultWebsitesState)
        assertTrue(state.isEmergencyOverrideActive)
        assertEquals(900L, state.overrideRemainingSeconds)
        assertEquals(0, state.activeModulesCount)
        assertFalse(state.isGuardingActive)
    }

    @Test
    fun testPolicy_emergencyOverride_expired_restoresProtection() {
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = true,
            blockYouTubeShorts = true,
            blockInstagramReels = true,
            blockAdultWebsites = true
        )

        val cal = createMondayCalendar(hour = 12, minute = 0)
        val now = cal.timeInMillis
        val expiredTimestamp = now - 5000L // 5 seconds in the past

        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = emptyList(),
            isEmergencyOverrideActive = true,
            overrideExpirationTimestamp = expiredTimestamp,
            accessibilityServiceConnected = true,
            hasAccessibilityPermission = true,
            vpnServiceConnected = true,
            hasVpnPermission = true,
            calendar = cal
        )

        // Expiration detected, normal protection restored
        assertEquals(OverallProtectionStatus.ACTIVE, state.overallStatus)
        assertEquals(ProtectionModuleState.ACTIVE, state.shortsState)
        assertEquals(ProtectionModuleState.ACTIVE, state.reelsState)
        assertEquals(ProtectionModuleState.ACTIVE, state.adultWebsitesState)
        assertFalse(state.isEmergencyOverrideActive)
        assertEquals(0L, state.overrideRemainingSeconds)
        assertTrue(state.isGuardingActive)
    }

    @Test
    fun testPolicy_serviceOffline_accessibilityDisconnected_reportsOffline() {
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = true,
            blockYouTubeShorts = true,
            blockInstagramReels = false,
            blockAdultWebsites = false
        )

        val cal = createMondayCalendar(hour = 12, minute = 0)
        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = emptyList(),
            isEmergencyOverrideActive = false,
            overrideExpirationTimestamp = 0L,
            accessibilityServiceConnected = false, // Accessibility disconnected!
            hasAccessibilityPermission = true,    // Permission was granted
            vpnServiceConnected = true,
            hasVpnPermission = true,
            calendar = cal
        )

        assertEquals(OverallProtectionStatus.OFFLINE, state.overallStatus)
        assertEquals(ProtectionModuleState.OFFLINE, state.shortsState)
    }

    @Test
    fun testPolicy_permissionMissing_accessibilityNotGranted_reportsPermissionRequired() {
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = true,
            blockYouTubeShorts = true,
            blockInstagramReels = false,
            blockAdultWebsites = false
        )

        val cal = createMondayCalendar(hour = 12, minute = 0)
        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = emptyList(),
            isEmergencyOverrideActive = false,
            overrideExpirationTimestamp = 0L,
            accessibilityServiceConnected = false,
            hasAccessibilityPermission = false, // User hasn't enabled service in Android settings!
            vpnServiceConnected = true,
            hasVpnPermission = true,
            calendar = cal
        )

        assertEquals(OverallProtectionStatus.PERMISSION_REQUIRED, state.overallStatus)
        assertEquals(ProtectionModuleState.PERMISSION_REQUIRED, state.shortsState)
    }

    @Test
    fun testPolicy_vpnOffline_reportsOffline() {
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = true,
            blockYouTubeShorts = false,
            blockInstagramReels = false,
            blockAdultWebsites = true // Adult website protection enabled
        )

        val cal = createMondayCalendar(hour = 12, minute = 0)
        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = emptyList(),
            isEmergencyOverrideActive = false,
            overrideExpirationTimestamp = 0L,
            accessibilityServiceConnected = true,
            hasAccessibilityPermission = true,
            vpnServiceConnected = false, // VPN not connected
            hasVpnPermission = true,
            calendar = cal
        )

        assertEquals(OverallProtectionStatus.OFFLINE, state.overallStatus)
        assertEquals(ProtectionModuleState.OFFLINE, state.adultWebsitesState)
    }

    @Test
    fun testPolicy_vpnPermissionMissing_reportsPermissionRequired() {
        val settings = ProtectionSettingsEntity(
            isMasterProtectionEnabled = true,
            blockYouTubeShorts = false,
            blockInstagramReels = false,
            blockAdultWebsites = true
        )

        val cal = createMondayCalendar(hour = 12, minute = 0)
        val state = ProtectionPolicyEngine.evaluatePolicy(
            protectionSettings = settings,
            schedules = emptyList(),
            isEmergencyOverrideActive = false,
            overrideExpirationTimestamp = 0L,
            accessibilityServiceConnected = true,
            hasAccessibilityPermission = true,
            vpnServiceConnected = false,
            hasVpnPermission = false, // VPN permission not yet granted
            calendar = cal
        )

        assertEquals(OverallProtectionStatus.PERMISSION_REQUIRED, state.overallStatus)
        assertEquals(ProtectionModuleState.PERMISSION_REQUIRED, state.adultWebsitesState)
    }

    @Test
    fun testStrictScopeCompliance_onlyThreePermittedModules() {
        // Enforces that exactly 3 modules are managed by ProtectionPolicyEngine
        val modules = listOf(
            ProtectionPolicyEngine.MODULE_SHORTS,
            ProtectionPolicyEngine.MODULE_REELS,
            ProtectionPolicyEngine.MODULE_ADULT
        )

        assertEquals(3, modules.size)
        assertTrue(modules.contains("MODULE_SHORTS"))
        assertTrue(modules.contains("MODULE_REELS"))
        assertTrue(modules.contains("MODULE_ADULT"))
        assertFalse(modules.any { it.contains("TIKTOK") })
        assertFalse(modules.any { it.contains("FACEBOOK") })
    }
}

