package com.dk.focusguard

import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import com.dk.focusguard.domain.policy.ProtectionPolicyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Comprehensive test suite verifying Focus Schedule enforcement logic,
 * overnight schedules crossing midnight, weekday matching, timezone resilience,
 * and multi-schedule transition calculations.
 */
class FocusScheduleEnforcementTest {

    private fun createCalendar(
        year: Int = 2026,
        month: Int = Calendar.NOVEMBER,
        day: Int,
        hour: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getTimeZone("UTC")
    ): Calendar {
        return Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testDaytimeSchedule_duringWindow_isActive() {
        val schedule = FocusScheduleEntity(
            id = 1,
            name = "Work Hours",
            startTime = "09:00",
            endTime = "17:00",
            selectedWeekdays = "Mon,Tue,Wed,Thu,Fri",
            isEnabled = true
        )

        // 2026-11-09 is Monday
        val monday10am = createCalendar(day = 9, hour = 10, minute = 0)
        assertTrue(ProtectionPolicyEngine.isScheduleActiveAt(schedule, monday10am))

        val monday1659 = createCalendar(day = 9, hour = 16, minute = 59)
        assertTrue(ProtectionPolicyEngine.isScheduleActiveAt(schedule, monday1659))
    }

    @Test
    fun testDaytimeSchedule_outsideWindow_isInactive() {
        val schedule = FocusScheduleEntity(
            id = 1,
            name = "Work Hours",
            startTime = "09:00",
            endTime = "17:00",
            selectedWeekdays = "Mon,Tue,Wed,Thu,Fri",
            isEnabled = true
        )

        // Monday before 09:00
        val monday859 = createCalendar(day = 9, hour = 8, minute = 59)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, monday859))

        // Monday at exactly 17:00 (end boundary is exclusive)
        val monday1700 = createCalendar(day = 9, hour = 17, minute = 0)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, monday1700))

        // Monday evening
        val monday2000 = createCalendar(day = 9, hour = 20, minute = 0)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, monday2000))
    }

    @Test
    fun testDaytimeSchedule_unselectedWeekday_isInactive() {
        val schedule = FocusScheduleEntity(
            id = 1,
            name = "Work Hours",
            startTime = "09:00",
            endTime = "17:00",
            selectedWeekdays = "Mon,Tue,Wed,Thu,Fri",
            isEnabled = true
        )

        // 2026-11-07 is Saturday
        val saturday11am = createCalendar(day = 7, hour = 11, minute = 0)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, saturday11am))

        // 2026-11-08 is Sunday
        val sunday14pm = createCalendar(day = 8, hour = 14, minute = 0)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, sunday14pm))
    }

    @Test
    fun testOvernightSchedule_crossingMidnight_part1Evening_isActive() {
        // Schedule: Friday 22:00 to 06:00
        val schedule = FocusScheduleEntity(
            id = 2,
            name = "Weekend Night Guard",
            startTime = "22:00",
            endTime = "06:00",
            selectedWeekdays = "Fri",
            isEnabled = true
        )

        // 2026-11-06 is Friday
        val friday2159 = createCalendar(day = 6, hour = 21, minute = 59)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, friday2159))

        val friday2200 = createCalendar(day = 6, hour = 22, minute = 0)
        assertTrue(ProtectionPolicyEngine.isScheduleActiveAt(schedule, friday2200))

        val friday2359 = createCalendar(day = 6, hour = 23, minute = 59)
        assertTrue(ProtectionPolicyEngine.isScheduleActiveAt(schedule, friday2359))
    }

    @Test
    fun testOvernightSchedule_crossingMidnight_part2Morning_isActive() {
        // Schedule: Friday 22:00 to 06:00 (Saturday is NOT in selectedWeekdays)
        val schedule = FocusScheduleEntity(
            id = 2,
            name = "Weekend Night Guard",
            startTime = "22:00",
            endTime = "06:00",
            selectedWeekdays = "Fri",
            isEnabled = true
        )

        // 2026-11-07 is Saturday
        // Morning belonging to Friday's overnight session:
        val saturdayMidnight = createCalendar(day = 7, hour = 0, minute = 0)
        assertTrue(ProtectionPolicyEngine.isScheduleActiveAt(schedule, saturdayMidnight))

        val saturday3am = createCalendar(day = 7, hour = 3, minute = 0)
        assertTrue(ProtectionPolicyEngine.isScheduleActiveAt(schedule, saturday3am))

        val saturday559am = createCalendar(day = 7, hour = 5, minute = 59)
        assertTrue(ProtectionPolicyEngine.isScheduleActiveAt(schedule, saturday559am))

        // At 06:00 window terminates:
        val saturday600am = createCalendar(day = 7, hour = 6, minute = 0)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, saturday600am))

        // Saturday evening is NOT selected:
        val saturday2300 = createCalendar(day = 7, hour = 23, minute = 0)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, saturday2300))

        // Sunday morning:
        val sunday3am = createCalendar(day = 8, hour = 3, minute = 0)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, sunday3am))
    }

    @Test
    fun testOvernightSchedule_multiDays_seamlessSpanning() {
        // Bedtime schedule: 22:00 to 07:00, Sun through Thu
        val schedule = FocusScheduleEntity(
            id = 3,
            name = "Bedtime Discipline",
            startTime = "22:00",
            endTime = "07:00",
            selectedWeekdays = "Sun,Mon,Tue,Wed,Thu",
            isEnabled = true
        )

        // Sunday 2026-11-08 at 22:30
        val sun2230 = createCalendar(day = 8, hour = 22, minute = 30)
        assertTrue(ProtectionPolicyEngine.isScheduleActiveAt(schedule, sun2230))

        // Monday 2026-11-09 at 05:00 (yesterday was Sunday, in schedule)
        val mon0500 = createCalendar(day = 9, hour = 5, minute = 0)
        assertTrue(ProtectionPolicyEngine.isScheduleActiveAt(schedule, mon0500))

        // Friday 2026-11-13 at 04:00 (yesterday was Thursday, in schedule!)
        val fri0400 = createCalendar(day = 13, hour = 4, minute = 0)
        assertTrue(ProtectionPolicyEngine.isScheduleActiveAt(schedule, fri0400))

        // Friday 2026-11-13 at 23:00 (Friday evening NOT in schedule)
        val fri2300 = createCalendar(day = 13, hour = 23, minute = 0)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, fri2300))
    }

    @Test
    fun testDisabledSchedule_alwaysInactive() {
        val schedule = FocusScheduleEntity(
            id = 4,
            name = "Disabled Routine",
            startTime = "00:00",
            endTime = "23:59",
            selectedWeekdays = "Mon,Tue,Wed,Thu,Fri,Sat,Sun",
            isEnabled = false
        )

        val monday10am = createCalendar(day = 9, hour = 10, minute = 0)
        assertFalse(ProtectionPolicyEngine.isScheduleActiveAt(schedule, monday10am))
    }

    @Test
    fun testMultipleSchedules_resolution() {
        val morningSchedule = FocusScheduleEntity(
            id = 1,
            name = "Morning Deep Work",
            startTime = "09:00",
            endTime = "12:00",
            selectedWeekdays = "Mon,Tue,Wed,Thu,Fri",
            isEnabled = true
        )
        val afternoonSchedule = FocusScheduleEntity(
            id = 2,
            name = "Afternoon Code Sprint",
            startTime = "14:00",
            endTime = "18:00",
            selectedWeekdays = "Mon,Tue,Wed,Thu,Fri",
            isEnabled = true
        )
        val schedules = listOf(morningSchedule, afternoonSchedule)

        // 10:30 Monday: morning is active
        val mon1030 = createCalendar(day = 9, hour = 10, minute = 30)
        val active1 = ProtectionPolicyEngine.getActiveSchedule(schedules, mon1030)
        assertNotNull(active1)
        assertEquals("Morning Deep Work", active1?.name)

        // 12:30 Monday: lunch gap, none active
        val mon1230 = createCalendar(day = 9, hour = 12, minute = 30)
        val active2 = ProtectionPolicyEngine.getActiveSchedule(schedules, mon1230)
        assertNull(active2)

        // 15:30 Monday: afternoon is active
        val mon1530 = createCalendar(day = 9, hour = 15, minute = 30)
        val active3 = ProtectionPolicyEngine.getActiveSchedule(schedules, mon1530)
        assertNotNull(active3)
        assertEquals("Afternoon Code Sprint", active3?.name)
    }

    @Test
    fun testCalculateNextTransition_daytimeSchedule() {
        val schedule = FocusScheduleEntity(
            id = 1,
            name = "Work Hours",
            startTime = "09:00",
            endTime = "17:00",
            selectedWeekdays = "Mon",
            isEnabled = true
        )

        // Monday 2026-11-09 at 10:00 -> next transition is Monday 17:00
        val monday10am = createCalendar(day = 9, hour = 10, minute = 0)
        val nextTransition = ProtectionPolicyEngine.calculateNextTransition(listOf(schedule), monday10am)
        assertNotNull(nextTransition)

        val expectedEndCal = createCalendar(day = 9, hour = 17, minute = 0)
        assertEquals(expectedEndCal.timeInMillis, nextTransition)
    }

    @Test
    fun testCalculateNextTransition_overnightSchedule() {
        val schedule = FocusScheduleEntity(
            id = 2,
            name = "Night Guard",
            startTime = "22:00",
            endTime = "06:00",
            selectedWeekdays = "Fri",
            isEnabled = true
        )

        // Friday 2026-11-06 at 20:00 -> next transition is Friday 22:00
        val fri2000 = createCalendar(day = 6, hour = 20, minute = 0)
        val nextStart = ProtectionPolicyEngine.calculateNextTransition(listOf(schedule), fri2000)
        assertNotNull(nextStart)
        val expectedStart = createCalendar(day = 6, hour = 22, minute = 0)
        assertEquals(expectedStart.timeInMillis, nextStart)

        // Friday 2026-11-06 at 23:00 -> next transition is Saturday 06:00
        val fri2300 = createCalendar(day = 6, hour = 23, minute = 0)
        val nextEnd = ProtectionPolicyEngine.calculateNextTransition(listOf(schedule), fri2300)
        assertNotNull(nextEnd)
        val expectedEnd = createCalendar(day = 7, hour = 6, minute = 0)
        assertEquals(expectedEnd.timeInMillis, nextEnd)
    }

    @Test
    fun testModuleEnforcementBySchedule_specificAndDefaultTargets() {
        val defaultSchedule = FocusScheduleEntity(
            id = 1,
            name = "All Defense",
            startTime = "09:00",
            endTime = "17:00",
            selectedWeekdays = "Mon",
            isEnabled = true,
            selectedAppPackages = ""
        )

        // Blank target string defaults to all 3 modules
        assertTrue(ProtectionPolicyEngine.isModuleEnforcedBySchedule(defaultSchedule, ProtectionPolicyEngine.MODULE_SHORTS))
        assertTrue(ProtectionPolicyEngine.isModuleEnforcedBySchedule(defaultSchedule, ProtectionPolicyEngine.MODULE_REELS))
        assertTrue(ProtectionPolicyEngine.isModuleEnforcedBySchedule(defaultSchedule, ProtectionPolicyEngine.MODULE_ADULT))

        val shortsOnlySchedule = FocusScheduleEntity(
            id = 2,
            name = "YouTube Focus",
            startTime = "09:00",
            endTime = "17:00",
            selectedWeekdays = "Mon",
            isEnabled = true,
            selectedAppPackages = "com.google.android.youtube"
        )
        assertTrue(ProtectionPolicyEngine.isModuleEnforcedBySchedule(shortsOnlySchedule, ProtectionPolicyEngine.MODULE_SHORTS))
        assertFalse(ProtectionPolicyEngine.isModuleEnforcedBySchedule(shortsOnlySchedule, ProtectionPolicyEngine.MODULE_REELS))
        assertFalse(ProtectionPolicyEngine.isModuleEnforcedBySchedule(shortsOnlySchedule, ProtectionPolicyEngine.MODULE_ADULT))
    }
}

