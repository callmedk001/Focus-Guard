package com.dk.focusguard.services

import kotlinx.coroutines.flow.StateFlow

/**
 * Architectural contract defining lifecycle controls for background protection.
 * Actual background enforcement (Accessibility/Usage/VPN) is decoupled and scheduled for future phases.
 */
interface ProtectionServiceContract {
    val serviceStatus: StateFlow<ServiceStatus>

    fun startProtection()
    fun stopProtection()
    fun pauseProtection(durationMinutes: Int)
}

