package com.dk.focusguard.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility functions for formatting date and time throughout the app.
 */
object DateTimeUtils {

    fun formatDuration(totalSeconds: Long): String {
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds)
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun formatDurationFromMillis(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%dh %02dm", hours, minutes)
        } else {
            String.format(Locale.getDefault(), "%d min", minutes)
        }
    }

    fun formatTimestamp(millis: Long, pattern: String = "MMM dd, yyyy HH:mm"): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(Date(millis))
    }

    fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
}

