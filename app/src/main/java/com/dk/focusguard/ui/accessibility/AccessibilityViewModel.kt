package com.dk.focusguard.ui.accessibility

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dk.focusguard.domain.repository.AccessibilityStatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccessibilityViewModel(
    private val accessibilityStatusRepository: AccessibilityStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessibilityUiState())
    val uiState: StateFlow<AccessibilityUiState> = _uiState.asStateFlow()

    init {
        observeStatus()
    }

    private fun observeStatus() {
        viewModelScope.launch {
            accessibilityStatusRepository.serviceState.collect { state ->
                _uiState.update { it.copy(serviceState = state) }
            }
        }

        viewModelScope.launch {
            accessibilityStatusRepository.isPermissionGranted.collect { granted ->
                _uiState.update { it.copy(isPermissionGranted = granted) }
            }
        }

        viewModelScope.launch {
            accessibilityStatusRepository.isServiceConnected.collect { connected ->
                _uiState.update { it.copy(isServiceConnected = connected) }
            }
        }

        viewModelScope.launch {
            accessibilityStatusRepository.currentForegroundPackage.collect { pkg ->
                _uiState.update { it.copy(currentForegroundPackage = pkg) }
            }
        }

        viewModelScope.launch {
            accessibilityStatusRepository.currentForegroundAppName.collect { appName ->
                _uiState.update { it.copy(currentForegroundAppName = appName) }
            }
        }

        viewModelScope.launch {
            accessibilityStatusRepository.lastEventTimestamp.collect { timestamp ->
                _uiState.update { it.copy(lastEventTimestamp = timestamp) }
            }
        }
    }

    fun refreshStatus(context: Context) {
        accessibilityStatusRepository.checkAndRefreshPermission(context)
    }

    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    companion object {
        fun provideFactory(
            accessibilityStatusRepository: AccessibilityStatusRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AccessibilityViewModel(accessibilityStatusRepository) as T
            }
        }
    }
}

