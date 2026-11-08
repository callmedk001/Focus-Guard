package com.dk.focusguard.ui.security

/**
 * Functional workflow modes for the security authentication screen.
 */
enum class AuthMode {
    SETUP,           // Initial setup: enter 4-6 digit PIN
    CONFIRM,         // Setup confirmation: verify matching PIN
    UNLOCK,          // Authorize protected settings or operations
    CHANGE_CURRENT,  // Verification: enter current PIN to authorize change
    CHANGE_NEW,      // Change workflow: enter replacement PIN
    CHANGE_CONFIRM   // Change workflow: confirm replacement PIN
}

/**
 * UI State representation for the Authentication Screen.
 */
data class AuthenticationUiState(
    val authMode: AuthMode = AuthMode.UNLOCK,
    val targetActionDescription: String = "Authorize Sensitive Operation",
    val enteredPin: String = "",
    val firstEnteredPin: String = "", // Used during SETUP and CHANGE flows for matching
    val errorMessage: String? = null,
    val isError: Boolean = false,
    val isSuccess: Boolean = false,
    val isLockedOut: Boolean = false,
    val remainingLockoutSeconds: Long = 0L,
    val failedAttempts: Int = 0,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isPinConfigured: Boolean = false
)

