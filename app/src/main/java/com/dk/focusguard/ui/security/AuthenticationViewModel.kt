package com.dk.focusguard.ui.security

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dk.focusguard.domain.repository.SecurityRepository
import com.dk.focusguard.security.BiometricAuthCallback
import com.dk.focusguard.security.BiometricAuthManager
import com.dk.focusguard.security.BiometricCapability
import com.dk.focusguard.security.PinSecurityManager
import com.dk.focusguard.security.SecurityLockManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val securityRepository: SecurityRepository,
    private val securityLockManager: SecurityLockManager,
    private val biometricAuthManager: BiometricAuthManager,
    initialMode: AuthMode = AuthMode.UNLOCK,
    actionDescription: String = "Authorize Action"
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthenticationUiState(
            authMode = initialMode,
            targetActionDescription = actionDescription
        )
    )
    val uiState: StateFlow<AuthenticationUiState> = _uiState.asStateFlow()

    private var lockoutTickerJob: Job? = null

    init {
        observeSecurityStatus()
        startLockoutTicker()
    }

    private fun observeSecurityStatus() {
        viewModelScope.launch {
            val biometricCapability = biometricAuthManager.checkBiometricCapability()
            val hasBiometrics = biometricCapability == BiometricCapability.AVAILABLE
            _uiState.update { it.copy(isBiometricAvailable = hasBiometrics) }
        }

        viewModelScope.launch {
            securityRepository.isBiometricEnabled.collect { enabled ->
                _uiState.update { it.copy(isBiometricEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            securityRepository.isPinSet.collect { isSet ->
                _uiState.update { it.copy(isPinConfigured = isSet) }
            }
        }

        viewModelScope.launch {
            securityRepository.failedAttemptCount.collect { count ->
                _uiState.update { it.copy(failedAttempts = count) }
            }
        }

        viewModelScope.launch {
            securityRepository.lockoutUntilTimestamp.collect { lockoutUntil ->
                updateLockoutState(lockoutUntil)
            }
        }
    }

    private fun updateLockoutState(lockoutUntil: Long) {
        val now = System.currentTimeMillis()
        val isLocked = securityLockManager.isLockedOut(lockoutUntil, now)
        val remaining = securityLockManager.getRemainingLockoutSeconds(lockoutUntil, now)

        _uiState.update {
            it.copy(
                isLockedOut = isLocked,
                remainingLockoutSeconds = remaining
            )
        }
    }

    private fun startLockoutTicker() {
        lockoutTickerJob?.cancel()
        lockoutTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val lockoutUntil = securityRepository.lockoutUntilTimestamp.first()
                val now = System.currentTimeMillis()
                if (now < lockoutUntil) {
                    val remaining = securityLockManager.getRemainingLockoutSeconds(lockoutUntil, now)
                    _uiState.update {
                        it.copy(
                            isLockedOut = true,
                            remainingLockoutSeconds = remaining
                        )
                    }
                } else if (_uiState.value.isLockedOut) {
                    _uiState.update {
                        it.copy(
                            isLockedOut = false,
                            remainingLockoutSeconds = 0L,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    fun onDigitClicked(digit: Char) {
        if (_uiState.value.isLockedOut) return
        val current = _uiState.value.enteredPin
        if (current.length < 6) {
            val updated = current + digit
            _uiState.update {
                it.copy(
                    enteredPin = updated,
                    isError = false,
                    errorMessage = null
                )
            }

            // If entering 4 or more digits in UNLOCK mode and max 6 reached, can auto-evaluate
            if (_uiState.value.authMode == AuthMode.UNLOCK && updated.length == 6) {
                verifyUnlockPin(updated)
            }
        }
    }

    fun onDeleteClicked() {
        if (_uiState.value.isLockedOut) return
        val current = _uiState.value.enteredPin
        if (current.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    enteredPin = current.dropLast(1),
                    isError = false,
                    errorMessage = null
                )
            }
        }
    }

    fun onClearClicked() {
        if (_uiState.value.isLockedOut) return
        _uiState.update {
            it.copy(
                enteredPin = "",
                isError = false,
                errorMessage = null
            )
        }
    }

    fun onContinueOrConfirmClicked() {
        if (_uiState.value.isLockedOut) return
        val pin = _uiState.value.enteredPin

        val validation = PinSecurityManager.validatePinFormat(pin)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    isError = true,
                    errorMessage = validation.errorMessage ?: "PIN must be 4–6 digits"
                )
            }
            return
        }

        when (_uiState.value.authMode) {
            AuthMode.SETUP -> {
                // Advance to confirmation step
                _uiState.update {
                    it.copy(
                        authMode = AuthMode.CONFIRM,
                        firstEnteredPin = pin,
                        enteredPin = "",
                        isError = false,
                        errorMessage = null
                    )
                }
            }

            AuthMode.CONFIRM -> {
                if (pin == _uiState.value.firstEnteredPin) {
                    saveNewPin(pin)
                } else {
                    _uiState.update {
                        it.copy(
                            isError = true,
                            errorMessage = "PINs do not match. Please try again.",
                            enteredPin = ""
                        )
                    }
                }
            }

            AuthMode.UNLOCK -> {
                verifyUnlockPin(pin)
            }

            AuthMode.CHANGE_CURRENT -> {
                verifyCurrentPinForChange(pin)
            }

            AuthMode.CHANGE_NEW -> {
                _uiState.update {
                    it.copy(
                        authMode = AuthMode.CHANGE_CONFIRM,
                        firstEnteredPin = pin,
                        enteredPin = "",
                        isError = false,
                        errorMessage = null
                    )
                }
            }

            AuthMode.CHANGE_CONFIRM -> {
                if (pin == _uiState.value.firstEnteredPin) {
                    saveNewPin(pin)
                } else {
                    _uiState.update {
                        it.copy(
                            isError = true,
                            errorMessage = "Replacement PINs do not match. Try again.",
                            enteredPin = ""
                        )
                    }
                }
            }
        }
    }

    private fun verifyUnlockPin(pin: String) {
        viewModelScope.launch {
            val lockoutUntil = securityRepository.lockoutUntilTimestamp.first()
            if (securityLockManager.isLockedOut(lockoutUntil)) {
                val remaining = securityLockManager.getRemainingLockoutSeconds(lockoutUntil)
                _uiState.update {
                    it.copy(
                        isLockedOut = true,
                        remainingLockoutSeconds = remaining,
                        errorMessage = "Lockout active. Wait $remaining seconds."
                    )
                }
                return@launch
            }

            val credentials = securityRepository.getStoredPinCredentials()
            if (credentials == null) {
                // No PIN set
                securityLockManager.authorizeSession()
                _uiState.update { it.copy(isSuccess = true) }
                return@launch
            }

            val (saltHex, hashHex) = credentials
            val isValid = PinSecurityManager.verifyPin(pin, saltHex, hashHex)

            if (isValid) {
                securityRepository.resetFailedAttempts()
                securityLockManager.authorizeSession()
                _uiState.update {
                    it.copy(
                        isSuccess = true,
                        isError = false,
                        errorMessage = null
                    )
                }
            } else {
                val currentAttempts = securityRepository.failedAttemptCount.first() + 1
                val lockoutSeconds = securityLockManager.calculateLockoutSeconds(currentAttempts)
                val newLockoutUntil = if (lockoutSeconds > 0) System.currentTimeMillis() + (lockoutSeconds * 1000L) else 0L

                securityRepository.recordFailedAttempt(currentAttempts, newLockoutUntil)

                val errorMsg = if (lockoutSeconds > 0) {
                    "Too many failed attempts. Locked out for $lockoutSeconds seconds."
                } else {
                    val remainingAttempts = 5 - currentAttempts
                    "Incorrect PIN. $remainingAttempts attempts remaining."
                }

                _uiState.update {
                    it.copy(
                        isError = true,
                        errorMessage = errorMsg,
                        enteredPin = "",
                        isLockedOut = lockoutSeconds > 0,
                        remainingLockoutSeconds = lockoutSeconds.toLong()
                    )
                }
            }
        }
    }

    private fun verifyCurrentPinForChange(pin: String) {
        viewModelScope.launch {
            val credentials = securityRepository.getStoredPinCredentials()
            if (credentials == null) {
                // If no existing PIN, proceed directly to new PIN
                _uiState.update {
                    it.copy(
                        authMode = AuthMode.CHANGE_NEW,
                        enteredPin = "",
                        errorMessage = null,
                        isError = false
                    )
                }
                return@launch
            }

            val (saltHex, hashHex) = credentials
            if (PinSecurityManager.verifyPin(pin, saltHex, hashHex)) {
                _uiState.update {
                    it.copy(
                        authMode = AuthMode.CHANGE_NEW,
                        enteredPin = "",
                        errorMessage = null,
                        isError = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isError = true,
                        errorMessage = "Current PIN incorrect.",
                        enteredPin = ""
                    )
                }
            }
        }
    }

    private fun saveNewPin(pin: String) {
        viewModelScope.launch {
            val hashResult = PinSecurityManager.hashPin(pin)
            val saveResult = securityRepository.savePin(hashResult.saltHex, hashResult.hashHex)
            if (saveResult.isSuccess) {
                securityLockManager.authorizeSession()
                _uiState.update {
                    it.copy(
                        isSuccess = true,
                        isError = false,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isError = true,
                        errorMessage = "Failed to securely save PIN. Please retry."
                    )
                }
            }
        }
    }

    fun triggerBiometricAuthentication(activity: Activity) {
        if (!_uiState.value.isBiometricEnabled || !_uiState.value.isBiometricAvailable) return
        if (_uiState.value.isLockedOut) return

        biometricAuthManager.authenticate(
            activity = activity,
            title = "DK FOCUS GUARD",
            subtitle = _uiState.value.targetActionDescription,
            negativeButtonText = "Use Master PIN",
            callback = object : BiometricAuthCallback {
                override fun onSuccess() {
                    viewModelScope.launch {
                        securityRepository.resetFailedAttempts()
                        securityLockManager.authorizeSession()
                        _uiState.update {
                            it.copy(
                                isSuccess = true,
                                isError = false,
                                errorMessage = null
                            )
                        }
                    }
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    _uiState.update {
                        it.copy(
                            isError = true,
                            errorMessage = errorMessage
                        )
                    }
                }

                override fun onCancel() {
                    // Fall back cleanly to PIN pad
                }
            }
        )
    }

    fun resetState() {
        _uiState.update {
            it.copy(
                enteredPin = "",
                isError = false,
                errorMessage = null,
                isSuccess = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        lockoutTickerJob?.cancel()
    }

    companion object {
        fun provideFactory(
            securityRepository: SecurityRepository,
            securityLockManager: SecurityLockManager,
            biometricAuthManager: BiometricAuthManager,
            initialMode: AuthMode = AuthMode.UNLOCK,
            actionDescription: String = "Authorize Sensitive Action"
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthenticationViewModel(
                    securityRepository = securityRepository,
                    securityLockManager = securityLockManager,
                    biometricAuthManager = biometricAuthManager,
                    initialMode = initialMode,
                    actionDescription = actionDescription
                ) as T
            }
        }
    }
}

