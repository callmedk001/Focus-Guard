package com.dk.focusguard

import com.dk.focusguard.utils.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeUtilsTest {

    @Test
    fun formatDuration_formatsMinutesAndSecondsCorrectly() {
        assertEquals("00:00", DateTimeUtils.formatDuration(0))
        assertEquals("00:59", DateTimeUtils.formatDuration(59))
        assertEquals("01:00", DateTimeUtils.formatDuration(60))
        assertEquals("25:00", DateTimeUtils.formatDuration(25 * 60))
        assertEquals("45:30", DateTimeUtils.formatDuration(45 * 60 + 30))
    }

    @Test
    fun formatDurationFromMillis_formatsHoursAndMinutes() {
        assertEquals("30 min", DateTimeUtils.formatDurationFromMillis(30 * 60 * 1000L))
        assertEquals("1h 15m", DateTimeUtils.formatDurationFromMillis((75 * 60 * 1000L)))
    }
}

