package com.dk.focusguard.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages runtime authentication state, temporary session authorization windows,
 * background lock triggers, and rate-limiting lockout calculations for DK FOCUS GUARD.
 */
class SecurityLockManager {

    companion object {
        const val DEFAULT_SESSION_DURATION_MILLIS = 120_000L // 2 minutes grace period
        const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
    }

    private val _isSessionAuthorized = MutableStateFlow(false)
    val isSessionAuthorized: StateFlow<Boolean> = _isSessionAuthorized.asStateFlow()

    private var sessionExpirationTimestamp: Long = 0L

    /**
     * Authorizes the current user session for a specific duration.
     * Prevents requiring repetitive authentication while navigating between protected settings.
     */
    fun authorizeSession(durationMillis: Long = DEFAULT_SESSION_DURATION_MILLIS) {
        sessionExpirationTimestamp = System.currentTimeMillis() + durationMillis
        _isSessionAuthorized.value = true
    }

    /**
     * Explicitly revokes active session authorization.
     */
    fun deauthorizeSession() {
        sessionExpirationTimestamp = 0L
        _isSessionAuthorized.value = false
    }

    /**
     * Checks if the active authorization session is still valid.
     * Automatically resets authorization if the inactivity period has elapsed.
     */
    fun isSessionValid(): Boolean {
        val now = System.currentTimeMillis()
        if (_isSessionAuthorized.value && now < sessionExpirationTimestamp) {
            return true
        }
        if (_isSessionAuthorized.value) {
            deauthorizeSession()
        }
        return false
    }

    /**
     * Invoked when the application transitions to the background.
     * Immediately locks sensitive screens and clears session authorization.
     */
    fun onAppBackgrounded() {
        deauthorizeSession()
    }

    /**
     * Computes the penalty lockout duration in seconds according to the count of consecutive failed attempts:
     * - 1 to 4 attempts: 0 seconds (warning only)
     * - 5 attempts: 30 seconds
     * - 6 attempts: 45 seconds
     * - 7 attempts: 60 seconds
     * - 8+ attempts: 300 seconds (5 minutes)
     */
    fun calculateLockoutSeconds(failedAttempts: Int): Int {
        return when {
            failedAttempts < 5 -> 0
            failedAttempts == 5 -> 30
            failedAttempts == 6 -> 45
            failedAttempts == 7 -> 60
            else -> 300
        }
    }

    /**
     * Returns true if the lockout penalty period is currently active.
     */
    fun isLockedOut(lockoutUntilTimestamp: Long, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        return currentTimeMillis < lockoutUntilTimestamp
    }

    /**
     * Calculates remaining seconds in the active lockout period.
     */
    fun getRemainingLockoutSeconds(lockoutUntilTimestamp: Long, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val remainingMillis = lockoutUntilTimestamp - currentTimeMillis
        return if (remainingMillis > 0) (remainingMillis + 999) / 1000 else 0L
    }
}

