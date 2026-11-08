package com.dk.focusguard.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dk.focusguard.domain.repository.BlockEventRepository
import com.dk.focusguard.domain.repository.FocusSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class StatisticsViewModel(
    private val blockEventRepository: BlockEventRepository,
    private val focusSessionRepository: FocusSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState(isLoading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var sampleDataActive = true

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        val weekStartMillis = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis

        viewModelScope.launch {
            combine(
                blockEventRepository.getTotalRealBlockEventsCount(),
                blockEventRepository.getRealEventCountSince(weekStartMillis),
                focusSessionRepository.getAllSessions(),
                focusSessionRepository.getTotalFocusDurationSince(weekStartMillis)
            ) { totalRealBlocks, weekRealBlocks, sessions, totalFocusSeconds ->
                val realFocusMinutes = (totalFocusSeconds / 60).toInt()
                val realCompletedSessions = sessions.filter { it.isCompleted }.size

                if (sampleDataActive) {
                    val sampleWeekly = listOf(
                        WeeklyDayStat("Mon", 4.5f, 1.5f),
                        WeeklyDayStat("Tue", 4.1f, 1.2f),
                        WeeklyDayStat("Wed", 3.2f, 2.0f),
                        WeeklyDayStat("Thu", 3.0f, 1.8f),
                        WeeklyDayStat("Fri", 2.8f, 2.5f, isToday = true),
                        WeeklyDayStat("Sat", 2.2f, 1.0f),
                        WeeklyDayStat("Sun", 1.8f, 1.2f)
                    )

                    val sampleApps = listOf(
                        AppUsageStat("YouTube", "com.google.android.youtube", "1h 12m", 0.32f, "Shorts Blocked", true),
                        AppUsageStat("Instagram", "com.instagram.android", "42m", 0.19f, "Reels Blocked", true),
                        AppUsageStat("Chrome", "com.android.chrome", "35m", 0.15f, "Web Filter Armed", false),
                        AppUsageStat("WhatsApp", "com.whatsapp", "28m", 0.12f, "Essential / Allowed", false),
                        AppUsageStat("Android Studio", "com.google.android.studio", "1h 45m", 0.45f, "Productive", false)
                    )

                    StatisticsUiState(
                        dailyScreenTimeFormatted = "3h 42m",
                        timeSavedFormatted = "1h 35m",
                        totalInterceptedToday = totalRealBlocks, // Real count (0 until Phase 3)
                        totalInterceptedWeek = weekRealBlocks, // Real count
                        totalFocusMinutesAchieved = realFocusMinutes.coerceAtLeast(45),
                        completedSessionsCount = realCompletedSessions.coerceAtLeast(sessions.size),
                        disciplineStreakDays = if (sessions.isNotEmpty()) 1 else 0,
                        weeklyChart = sampleWeekly,
                        mostUsedApps = sampleApps,
                        isDemoData = true,
                        realBlockEventsCount = totalRealBlocks,
                        realSessionsCount = sessions.size,
                        isLoading = false
                    )
                } else {
                    // Pure Real Data Mode
                    StatisticsUiState(
                        dailyScreenTimeFormatted = "0m",
                        timeSavedFormatted = "0m",
                        totalInterceptedToday = totalRealBlocks,
                        totalInterceptedWeek = weekRealBlocks,
                        totalFocusMinutesAchieved = realFocusMinutes,
                        completedSessionsCount = realCompletedSessions,
                        disciplineStreakDays = if (sessions.isNotEmpty()) 1 else 0,
                        weeklyChart = emptyList(),
                        mostUsedApps = emptyList(),
                        isDemoData = false,
                        realBlockEventsCount = totalRealBlocks,
                        realSessionsCount = sessions.size,
                        isLoading = false
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun clearSampleStatistics() {
        sampleDataActive = false
        loadStatistics()
    }

    companion object {
        fun provideFactory(
            blockEventRepository: BlockEventRepository,
            focusSessionRepository: FocusSessionRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StatisticsViewModel(
                    blockEventRepository,
                    focusSessionRepository
                ) as T
            }
        }
    }
}
