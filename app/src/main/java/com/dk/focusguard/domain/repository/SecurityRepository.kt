package com.dk.focusguard.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface governing persistent security metadata,
 * authentication preferences, PIN hash records, and rate limiting states.
 */
interface SecurityRepository {

    val isPinSet: Flow<Boolean>

    val isBiometricEnabled: Flow<Boolean>

    val failedAttemptCount: Flow<Int>

    val lockoutUntilTimestamp: Flow<Long>

    val isEmergencyOverrideActive: Flow<Boolean>

    val emergencyOverrideExpiration: Flow<Long>

    val autoLockInactivitySeconds: Flow<Int>

    val requireAuthForProtection: Flow<Boolean>

    val requireAuthForDataReset: Flow<Boolean>

    suspend fun getStoredPinCredentials(): Pair<String, String>? // (saltHex, hashHex)

    suspend fun savePin(saltHex: String, hashHex: String): Result<Unit>

    suspend fun clearPin(): Result<Unit>

    suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit>

    suspend fun recordFailedAttempt(newCount: Int, lockoutUntilTimestamp: Long): Result<Unit>

    suspend fun resetFailedAttempts(): Result<Unit>

    suspend fun setEmergencyOverride(active: Boolean, expirationTimestamp: Long): Result<Unit>

    suspend fun setAutoLockInactivitySeconds(seconds: Int): Result<Unit>

    suspend fun setRequireAuthForProtection(required: Boolean): Result<Unit>

    suspend fun setRequireAuthForDataReset(required: Boolean): Result<Unit>
}

