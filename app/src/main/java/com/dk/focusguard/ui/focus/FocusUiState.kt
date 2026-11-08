package com.dk.focusguard.ui.focus

import com.dk.focusguard.domain.model.FocusSession

enum class FocusTimerState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class FocusUiState(
    val timerState: FocusTimerState = FocusTimerState.IDLE,
    val selectedDurationMinutes: Int = 25,
    val remainingSeconds: Long = 25 * 60L,
    val totalSeconds: Long = 25 * 60L,
    val sessionTitle: String = "Cyber Defense Focus Session",
    val recentSessions: List<FocusSession> = emptyList(),

    // Scheduled Focus Mode
    val isScheduleEnabled: Boolean = false,
    val scheduledStartTime: String = "09:00",
    val scheduledEndTime: String = "17:00",
    val selectedDays: Set<String> = setOf("Mon", "Tue", "Wed", "Thu", "Fri"),
    val selectedAppsForFocus: Set<String> = setOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.dk.focusguard.adult_filter"
    ),
    val availableApps: List<Pair<String, String>> = listOf(
        "YouTube (Shorts Shield)" to "com.google.android.youtube",
        "Instagram (Reels Guard)" to "com.instagram.android",
        "Adult Content (DNS Filter)" to "com.dk.focusguard.adult_filter"
    ),
    val scheduleSaveFeedback: String? = null,
    val isLoading: Boolean = false
)
