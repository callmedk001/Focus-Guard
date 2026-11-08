package com.dk.focusguard.fakes

import com.dk.focusguard.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory test double for SecurityRepository.
 */
class FakeSecurityRepository : SecurityRepository {
    private var storedSalt: String? = null
    private var storedHash: String? = null

    private val _isPinSet = MutableStateFlow(false)
    override val isPinSet: Flow<Boolean> = _isPinSet.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    override val isBiometricEnabled: Flow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _failedAttemptCount = MutableStateFlow(0)
    override val failedAttemptCount: Flow<Int> = _failedAttemptCount.asStateFlow()

    private val _lockoutUntilTimestamp = MutableStateFlow(0L)
    override val lockoutUntilTimestamp: Flow<Long> = _lockoutUntilTimestamp.asStateFlow()

    private val _isEmergencyOverrideActive = MutableStateFlow(false)
    override val isEmergencyOverrideActive: Flow<Boolean> = _isEmergencyOverrideActive.asStateFlow()

    private val _emergencyOverrideExpiration = MutableStateFlow(0L)
    override val emergencyOverrideExpiration: Flow<Long> = _emergencyOverrideExpiration.asStateFlow()

    private val _autoLockInactivitySeconds = MutableStateFlow(120)
    override val autoLockInactivitySeconds: Flow<Int> = _autoLockInactivitySeconds.asStateFlow()

    private val _requireAuthForProtection = MutableStateFlow(true)
    override val requireAuthForProtection: Flow<Boolean> = _requireAuthForProtection.asStateFlow()

    private val _requireAuthForDataReset = MutableStateFlow(true)
    override val requireAuthForDataReset: Flow<Boolean> = _requireAuthForDataReset.asStateFlow()

    override suspend fun getStoredPinCredentials(): Pair<String, String>? {
        return if (storedSalt != null && storedHash != null) {
            Pair(storedSalt!!, storedHash!!)
        } else null
    }

    override suspend fun savePin(saltHex: String, hashHex: String): Result<Unit> {
        storedSalt = saltHex
        storedHash = hashHex
        _isPinSet.value = true
        _failedAttemptCount.value = 0
        _lockoutUntilTimestamp.value = 0L
        return Result.success(Unit)
    }

    override suspend fun clearPin(): Result<Unit> {
        storedSalt = null
        storedHash = null
        _isPinSet.value = false
        _isBiometricEnabled.value = false
        _failedAttemptCount.value = 0
        _lockoutUntilTimestamp.value = 0L
        return Result.success(Unit)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit> {
        _isBiometricEnabled.value = enabled
        return Result.success(Unit)
    }

    override suspend fun recordFailedAttempt(newCount: Int, lockoutUntilTimestamp: Long): Result<Unit> {
        _failedAttemptCount.value = newCount
        _lockoutUntilTimestamp.value = lockoutUntilTimestamp
        return Result.success(Unit)
    }

    override suspend fun resetFailedAttempts(): Result<Unit> {
        _failedAttemptCount.value = 0
        _lockoutUntilTimestamp.value = 0L
        return Result.success(Unit)
    }

    override suspend fun setEmergencyOverride(active: Boolean, expirationTimestamp: Long): Result<Unit> {
        _isEmergencyOverrideActive.value = active
        _emergencyOverrideExpiration.value = expirationTimestamp
        return Result.success(Unit)
    }

    override suspend fun setAutoLockInactivitySeconds(seconds: Int): Result<Unit> {
        _autoLockInactivitySeconds.value = seconds
        return Result.success(Unit)
    }

    override suspend fun setRequireAuthForProtection(required: Boolean): Result<Unit> {
        _requireAuthForProtection.value = required
        return Result.success(Unit)
    }

    override suspend fun setRequireAuthForDataReset(required: Boolean): Result<Unit> {
        _requireAuthForDataReset.value = required
        return Result.success(Unit)
    }
}

