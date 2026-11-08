package com.dk.focusguard.domain.policy

import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import com.dk.focusguard.data.local.entity.ProtectionSettingsEntity
import com.dk.focusguard.domain.model.OverallProtectionStatus
import com.dk.focusguard.domain.model.ProtectionModuleState
import com.dk.focusguard.domain.model.UnifiedProtectionState
import java.util.Calendar

/**
 * Pure policy evaluation engine for DK FOCUS GUARD.
 *
 * Responsibilities:
 * 1. Focus schedule time matching across 24h & midnight boundaries.
 * 2. Multi-schedule resolution & next transition computation.
 * 3. Emergency override prioritization.
 * 4. Aggregates module and overall health states (ACTIVE, INACTIVE, SCHEDULED, OFFLINE, PERMISSION_REQUIRED).
 */
object ProtectionPolicyEngine {

    const val MODULE_SHORTS = "MODULE_SHORTS"
    const val MODULE_REELS = "MODULE_REELS"
    const val MODULE_ADULT = "MODULE_ADULT"

    /**
     * Parses "HH:mm" time string into minutes from midnight (0..1439).
     */
    fun parseTimeToMinutes(timeStr: String): Int? {
        val trimmed = timeStr.trim()
        val parts = trimmed.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    /**
     * Maps Calendar day of week to standardized 3-letter weekday code ("Mon", "Tue", ...).
     */
    fun getWeekdayCode(calendar: Calendar): String = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        Calendar.SUNDAY -> "Sun"
        else -> "Mon"
    }

    /**
     * Gets the 3-letter weekday code for the day preceding the calendar's current date.
     */
    fun getPreviousWeekdayCode(calendar: Calendar): String {
        val prevCal = calendar.clone() as Calendar
        prevCal.add(Calendar.DAY_OF_MONTH, -1)
        return getWeekdayCode(prevCal)
    }

    /**
     * Determines whether the given schedule is actively in effect at the provided Calendar moment.
     * Accurately supports overnight schedules crossing midnight (e.g. 22:00 to 06:00).
     */
    fun isScheduleActiveAt(schedule: FocusScheduleEntity, calendar: Calendar): Boolean {
        if (!schedule.isEnabled) return false
        val startMinutes = parseTimeToMinutes(schedule.startTime) ?: return false
        val endMinutes = parseTimeToMinutes(schedule.endTime) ?: return false

        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val todayCode = getWeekdayCode(calendar)
        val yesterdayCode = getPreviousWeekdayCode(calendar)

        val selectedDays = schedule.selectedWeekdays
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        fun matchesDay(dayCode: String): Boolean {
            return selectedDays.any { selected ->
                selected.equals(dayCode, ignoreCase = true) ||
                selected.startsWith(dayCode, ignoreCase = true)
            }
        }

        return if (startMinutes < endMinutes) {
            // Standard daytime window on the same calendar day (e.g. 09:00 to 17:00)
            matchesDay(todayCode) && currentMinutes >= startMinutes && currentMinutes < endMinutes
        } else if (startMinutes > endMinutes) {
            // Overnight window spanning midnight (e.g. 22:00 to 06:00)
            // Part 1: Evening of day 1 (currentMinutes >= startMinutes), applies if today was selected
            // Part 2: Morning of day 2 (currentMinutes < endMinutes), applies if yesterday was selected!
            (matchesDay(todayCode) && currentMinutes >= startMinutes) ||
            (matchesDay(yesterdayCode) && currentMinutes < endMinutes)
        } else {
            // startMinutes == endMinutes: 24-hour schedule if today matches
            matchesDay(todayCode)
        }
    }

    /**
     * Returns the primary active schedule at the current moment, or null if none are active.
     */
    fun getActiveSchedule(schedules: List<FocusScheduleEntity>, calendar: Calendar): FocusScheduleEntity? {
        return schedules.firstOrNull { isScheduleActiveAt(it, calendar) }
    }

    /**
     * Checks if a specific module is enforced by an active schedule.
     * Target packages can specify "youtube", "instagram", "adult"/"vpn", or empty/blank (defaulting to all 3).
     */
    fun isModuleEnforcedBySchedule(schedule: FocusScheduleEntity, moduleKey: String): Boolean {
        if (schedule.selectedAppPackages.isBlank()) {
            return true
        }
        val pkgs = schedule.selectedAppPackages.split(",").map { it.trim().lowercase() }
        return when (moduleKey) {
            MODULE_SHORTS -> pkgs.any { it.contains("youtube") || it.contains("shorts") || it == "all" }
            MODULE_REELS -> pkgs.any { it.contains("instagram") || it.contains("reels") || it == "all" }
            MODULE_ADULT -> pkgs.any { it.contains("adult") || it.contains("dns") || it.contains("vpn") || it == "all" }
            else -> false
        }
    }

    /**
     * Calculates the exact next schedule transition (in epoch milliseconds) when any enabled schedule
     * will either start or terminate, within an 8-day forward horizon.
     */
    fun calculateNextTransition(schedules: List<FocusScheduleEntity>, calendar: Calendar): Long? {
        val enabledSchedules = schedules.filter { it.isEnabled }
        if (enabledSchedules.isEmpty()) return null

        val currentTimeMillis = calendar.timeInMillis
        val candidateTransitions = mutableListOf<Long>()

        for (schedule in enabledSchedules) {
            val startMinutes = parseTimeToMinutes(schedule.startTime) ?: continue
            val endMinutes = parseTimeToMinutes(schedule.endTime) ?: continue

            val selectedDays = schedule.selectedWeekdays
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            fun matchesDay(dayCode: String): Boolean {
                return selectedDays.any { selected ->
                    selected.equals(dayCode, ignoreCase = true) ||
                    selected.startsWith(dayCode, ignoreCase = true)
                }
            }

            // Inspect candidate days from today (day 0) through day 7
            for (dayOffset in 0..7) {
                val dayCal = (calendar.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                }
                val dayCode = getWeekdayCode(dayCal)

                if (startMinutes < endMinutes) {
                    if (matchesDay(dayCode)) {
                        // Start candidate
                        val startCal = (dayCal.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, startMinutes / 60)
                            set(Calendar.MINUTE, startMinutes % 60)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (startCal.timeInMillis > currentTimeMillis) {
                            candidateTransitions.add(startCal.timeInMillis)
                        }

                        // End candidate
                        val endCal = (dayCal.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, endMinutes / 60)
                            set(Calendar.MINUTE, endMinutes % 60)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (endCal.timeInMillis > currentTimeMillis) {
                            candidateTransitions.add(endCal.timeInMillis)
                        }
                    }
                } else if (startMinutes > endMinutes) {
                    // Overnight window
                    if (matchesDay(dayCode)) {
                        // Start candidate on dayCal evening
                        val startCal = (dayCal.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, startMinutes / 60)
                            set(Calendar.MINUTE, startMinutes % 60)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (startCal.timeInMillis > currentTimeMillis) {
                            candidateTransitions.add(startCal.timeInMillis)
                        }

                        // End candidate on the next morning (dayCal + 1)
                        val endCal = (dayCal.clone() as Calendar).apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, endMinutes / 60)
                            set(Calendar.MINUTE, endMinutes % 60)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (endCal.timeInMillis > currentTimeMillis) {
                            candidateTransitions.add(endCal.timeInMillis)
                        }
                    }

                    // Special check for dayOffset == 0: could yesterday's overnight window end today?
                    if (dayOffset == 0) {
                        val yesterdayCal = (calendar.clone() as Calendar).apply {
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        if (matchesDay(getWeekdayCode(yesterdayCal))) {
                            val todayEndCal = (calendar.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, endMinutes / 60)
                                set(Calendar.MINUTE, endMinutes % 60)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            if (todayEndCal.timeInMillis > currentTimeMillis) {
                                candidateTransitions.add(todayEndCal.timeInMillis)
                            }
                        }
                    }
                }
            }
        }

        return candidateTransitions.filter { it > currentTimeMillis }.minOrNull()
    }

    /**
     * Evaluates the unified protection posture based on all current settings, schedules,
     * security overrides, and subsystem connectivity.
     */
    fun evaluatePolicy(
        protectionSettings: ProtectionSettingsEntity,
        schedules: List<FocusScheduleEntity>,
        isEmergencyOverrideActive: Boolean,
        overrideExpirationTimestamp: Long,
        accessibilityServiceConnected: Boolean,
        hasAccessibilityPermission: Boolean,
        vpnServiceConnected: Boolean,
        hasVpnPermission: Boolean,
        calendar: Calendar = Calendar.getInstance()
    ): UnifiedProtectionState {
        val currentTimeMillis = calendar.timeInMillis

        // 1. Check Emergency Override
        val overrideActive = isEmergencyOverrideActive && currentTimeMillis < overrideExpirationTimestamp
        val overrideRemainingSeconds = if (overrideActive) {
            ((overrideExpirationTimestamp - currentTimeMillis) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }

        // 2. Check Active Schedules
        val activeSchedules = schedules.filter { isScheduleActiveAt(it, calendar) }
        val isScheduleActive = activeSchedules.isNotEmpty()
        val activeScheduleName = activeSchedules.firstOrNull()?.name
        val nextTransitionMillis = calculateNextTransition(schedules, calendar)

        if (overrideActive) {
            return UnifiedProtectionState(
                overallStatus = OverallProtectionStatus.OVERRIDE_ACTIVE,
                shortsState = ProtectionModuleState.INACTIVE,
                reelsState = ProtectionModuleState.INACTIVE,
                adultWebsitesState = ProtectionModuleState.INACTIVE,
                isEmergencyOverrideActive = true,
                overrideExpirationTimestamp = overrideExpirationTimestamp,
                overrideRemainingSeconds = overrideRemainingSeconds,
                activeScheduleName = activeScheduleName,
                isScheduleActive = isScheduleActive,
                nextScheduleTransitionMillis = nextTransitionMillis,
                accessibilityServiceConnected = accessibilityServiceConnected,
                vpnServiceConnected = vpnServiceConnected,
                hasAccessibilityPermission = hasAccessibilityPermission,
                hasVpnPermission = hasVpnPermission,
                activeModulesCount = 0
            )
        }

        // 3. Determine if each module SHOULD be enforced
        val shouldEnforceShorts = if (isScheduleActive) {
            activeSchedules.any { isModuleEnforcedBySchedule(it, MODULE_SHORTS) }
        } else {
            protectionSettings.isMasterProtectionEnabled && protectionSettings.blockYouTubeShorts
        }

        val shouldEnforceReels = if (isScheduleActive) {
            activeSchedules.any { isModuleEnforcedBySchedule(it, MODULE_REELS) }
        } else {
            protectionSettings.isMasterProtectionEnabled && protectionSettings.blockInstagramReels
        }

        val shouldEnforceAdult = if (isScheduleActive) {
            activeSchedules.any { isModuleEnforcedBySchedule(it, MODULE_ADULT) }
        } else {
            protectionSettings.isMasterProtectionEnabled && protectionSettings.blockAdultWebsites
        }

        // 4. Determine state for each module
        fun resolveModuleState(
            shouldEnforce: Boolean,
            isConnected: Boolean,
            hasPermission: Boolean,
            isScheduled: Boolean
        ): ProtectionModuleState {
            if (!shouldEnforce) return ProtectionModuleState.INACTIVE
            return when {
                isConnected -> if (isScheduled) ProtectionModuleState.SCHEDULED else ProtectionModuleState.ACTIVE
                !hasPermission -> ProtectionModuleState.PERMISSION_REQUIRED
                else -> ProtectionModuleState.OFFLINE
            }
        }

        val shortsState = resolveModuleState(
            shouldEnforce = shouldEnforceShorts,
            isConnected = accessibilityServiceConnected,
            hasPermission = hasAccessibilityPermission,
            isScheduled = isScheduleActive && activeSchedules.any { isModuleEnforcedBySchedule(it, MODULE_SHORTS) }
        )

        val reelsState = resolveModuleState(
            shouldEnforce = shouldEnforceReels,
            isConnected = accessibilityServiceConnected,
            hasPermission = hasAccessibilityPermission,
            isScheduled = isScheduleActive && activeSchedules.any { isModuleEnforcedBySchedule(it, MODULE_REELS) }
        )

        val adultState = resolveModuleState(
            shouldEnforce = shouldEnforceAdult,
            isConnected = vpnServiceConnected,
            hasPermission = hasVpnPermission,
            isScheduled = isScheduleActive && activeSchedules.any { isModuleEnforcedBySchedule(it, MODULE_ADULT) }
        )

        val moduleStates = listOf(shortsState, reelsState, adultState)
        val activeCount = moduleStates.count {
            it == ProtectionModuleState.ACTIVE || it == ProtectionModuleState.SCHEDULED
        }

        // 5. Determine Overall Protection Status
        val overallStatus = when {
            moduleStates.any { it == ProtectionModuleState.PERMISSION_REQUIRED } ->
                OverallProtectionStatus.PERMISSION_REQUIRED
            moduleStates.any { it == ProtectionModuleState.OFFLINE } ->
                OverallProtectionStatus.OFFLINE
            moduleStates.any { it == ProtectionModuleState.SCHEDULED } ->
                OverallProtectionStatus.SCHEDULED
            moduleStates.any { it == ProtectionModuleState.ACTIVE } ->
                OverallProtectionStatus.ACTIVE
            else ->
                OverallProtectionStatus.INACTIVE
        }

        return UnifiedProtectionState(
            overallStatus = overallStatus,
            shortsState = shortsState,
            reelsState = reelsState,
            adultWebsitesState = adultState,
            isEmergencyOverrideActive = false,
            overrideExpirationTimestamp = 0L,
            overrideRemainingSeconds = 0L,
            activeScheduleName = activeScheduleName,
            isScheduleActive = isScheduleActive,
            nextScheduleTransitionMillis = nextTransitionMillis,
            accessibilityServiceConnected = accessibilityServiceConnected,
            vpnServiceConnected = vpnServiceConnected,
            hasAccessibilityPermission = hasAccessibilityPermission,
            hasVpnPermission = hasVpnPermission,
            activeModulesCount = activeCount
        )
    }
}

