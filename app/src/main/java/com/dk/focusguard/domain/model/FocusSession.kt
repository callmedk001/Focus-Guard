package com.dk.focusguard.domain.model

/**
 * Domain model representing a focused work session.
 */
data class FocusSession(
    val id: Long = 0,
    val title: String,
    val plannedDurationMinutes: Int,
    val actualDurationSeconds: Long = 0,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long? = null,
    val isCompleted: Boolean = false,
    val distractionsIntercepted: Int = 0
)

