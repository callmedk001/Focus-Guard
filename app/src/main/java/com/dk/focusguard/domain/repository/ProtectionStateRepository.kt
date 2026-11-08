package com.dk.focusguard.domain.repository

import com.dk.focusguard.domain.model.UnifiedProtectionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for unified protection state across all modules,
 * focus schedules, and emergency overrides.
 */
interface ProtectionStateRepository {
    val protectionState: StateFlow<UnifiedProtectionState>
    val currentProtectionState: UnifiedProtectionState
    suspend fun refreshProtectionState()
}

