package com.dk.focusguard.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import com.dk.focusguard.data.local.entity.FocusSessionEntity
import com.dk.focusguard.domain.repository.FocusRepository
import com.dk.focusguard.domain.repository.FocusScheduleRepository
import com.dk.focusguard.domain.repository.FocusSessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FocusViewModel(
    private val focusSessionRepository: FocusSessionRepository,
    private val focusScheduleRepository: FocusScheduleRepository,
    private val legacyFocusRepository: FocusRepository? = null,
    private val unifiedProtectionManager: com.dk.focusguard.domain.manager.UnifiedProtectionManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadRecentSessions()
        loadSavedSchedule()
    }

    private fun loadRecentSessions() {
        viewModelScope.launch {
            focusSessionRepository.getRecentSessions(10).collect { sessions ->
                _uiState.update { current ->
                    current.copy(recentSessions = sessions.map { it.toDomain() })
                }
            }
        }
    }

    private fun loadSavedSchedule() {
        viewModelScope.launch {
            focusScheduleRepository.getAllSchedules().collect { schedules ->
                val primarySchedule = schedules.firstOrNull()
                if (primarySchedule != null) {
                    val days = primarySchedule.selectedWeekdays.split(",").filter { it.isNotBlank() }.toSet()
                    val apps = primarySchedule.selectedAppPackages.split(",").filter { it.isNotBlank() }.toSet()
                    _uiState.update { current ->
                        current.copy(
                            isScheduleEnabled = primarySchedule.isEnabled,
                            scheduledStartTime = primarySchedule.startTime,
                            scheduledEndTime = primarySchedule.endTime,
                            selectedDays = if (days.isNotEmpty()) days else current.selectedDays,
                            selectedAppsForFocus = if (apps.isNotEmpty()) apps else current.selectedAppsForFocus
                        )
                    }
                }
            }
        }
    }

    fun selectDuration(minutes: Int) {
        if (_uiState.value.timerState == FocusTimerState.IDLE) {
            _uiState.update {
                it.copy(
                    selectedDurationMinutes = minutes,
                    remainingSeconds = minutes * 60L,
                    totalSeconds = minutes * 60L
                )
            }
        }
    }

    fun startTimer() {
        if (_uiState.value.timerState == FocusTimerState.RUNNING) return

        _uiState.update { it.copy(timerState = FocusTimerState.RUNNING) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 && _uiState.value.timerState == FocusTimerState.RUNNING) {
                delay(1000L)
                _uiState.update { current ->
                    val nextRemaining = current.remainingSeconds - 1
                    if (nextRemaining <= 0) {
                        current.copy(
                            remainingSeconds = 0,
                            timerState = FocusTimerState.COMPLETED
                        )
                    } else {
                        current.copy(remainingSeconds = nextRemaining)
                    }
                }
            }

            if (_uiState.value.timerState == FocusTimerState.COMPLETED) {
                saveCompletedSession()
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timerState = FocusTimerState.PAUSED) }
    }

    fun resumeTimer() {
        startTimer()
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                timerState = FocusTimerState.IDLE,
                remainingSeconds = it.selectedDurationMinutes * 60L
            )
        }
    }

    private fun saveCompletedSession() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val entity = FocusSessionEntity(
                title = currentState.sessionTitle,
                plannedDurationMinutes = currentState.selectedDurationMinutes,
                actualDurationSeconds = currentState.totalSeconds,
                startTimeMillis = System.currentTimeMillis() - (currentState.totalSeconds * 1000L),
                endTimeMillis = System.currentTimeMillis(),
                isCompleted = true,
                status = "COMPLETED",
                distractionsIntercepted = 0
            )
            focusSessionRepository.recordSession(entity)
            legacyFocusRepository?.createSession(entity.toDomain())
        }
    }

    // Schedule Controls
    fun toggleScheduleEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isScheduleEnabled = enabled) }
        viewModelScope.launch {
            val current = _uiState.value
            focusScheduleRepository.saveSchedule(
                FocusScheduleEntity(
                    id = 1,
                    name = "Daily Work Focus",
                    startTime = current.scheduledStartTime,
                    endTime = current.scheduledEndTime,
                    selectedWeekdays = current.selectedDays.joinToString(","),
                    isEnabled = enabled,
                    selectedAppPackages = current.selectedAppsForFocus.joinToString(",")
                )
            )
            unifiedProtectionManager?.evaluateAndEnforce()
        }
    }

    fun updateStartTime(time: String) {
        _uiState.update { it.copy(scheduledStartTime = time) }
    }

    fun updateEndTime(time: String) {
        _uiState.update { it.copy(scheduledEndTime = time) }
    }

    fun toggleDay(day: String) {
        _uiState.update { current ->
            val updated = current.selectedDays.toMutableSet()
            if (updated.contains(day)) updated.remove(day) else updated.add(day)
            current.copy(selectedDays = updated)
        }
    }

    fun toggleAppSelection(packageName: String) {
        _uiState.update { current ->
            val updated = current.selectedAppsForFocus.toMutableSet()
            if (updated.contains(packageName)) updated.remove(packageName) else updated.add(packageName)
            current.copy(selectedAppsForFocus = updated)
        }
    }

    fun saveSchedule() {
        viewModelScope.launch {
            val current = _uiState.value
            val entity = FocusScheduleEntity(
                id = 1,
                name = "Daily Work Focus",
                startTime = current.scheduledStartTime,
                endTime = current.scheduledEndTime,
                selectedWeekdays = current.selectedDays.joinToString(","),
                isEnabled = current.isScheduleEnabled,
                selectedAppPackages = current.selectedAppsForFocus.joinToString(",")
            )
            focusScheduleRepository.saveSchedule(entity)
            unifiedProtectionManager?.evaluateAndEnforce()

            _uiState.update {
                it.copy(scheduleSaveFeedback = "Focus Schedule saved to local database.")
            }
            delay(3000L)
            _uiState.update { it.copy(scheduleSaveFeedback = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        fun provideFactory(
            focusSessionRepository: FocusSessionRepository,
            focusScheduleRepository: FocusScheduleRepository,
            legacyFocusRepository: FocusRepository? = null,
            unifiedProtectionManager: com.dk.focusguard.domain.manager.UnifiedProtectionManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FocusViewModel(
                    focusSessionRepository,
                    focusScheduleRepository,
                    legacyFocusRepository,
                    unifiedProtectionManager
                ) as T
            }
        }
    }
}
