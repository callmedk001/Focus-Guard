package com.dk.focusguard.security

/**
 * Architectural contract for security, anti-tampering, and authentication.
 * Implementation will be integrated in future phases per roadmap.
 */
interface SecurityContract {
    val securityLevel: SecurityLevel
    val isAppLocked: Boolean

    fun requiresAuthentication(): Boolean
}

