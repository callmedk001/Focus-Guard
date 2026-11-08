package com.dk.focusguard.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dk.focusguard.data.preferences.dataStore
import com.dk.focusguard.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

class SecurityRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SecurityRepository {

    constructor(context: Context) : this(context.dataStore)

    private object Keys {
        val PIN_SALT = stringPreferencesKey("security_pin_salt")
        val PIN_HASH = stringPreferencesKey("security_pin_hash")
        val IS_PIN_SET = booleanPreferencesKey("security_is_pin_set")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("security_biometric_enabled")
        val FAILED_ATTEMPT_COUNT = intPreferencesKey("security_failed_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("security_lockout_until")
        val EMERGENCY_OVERRIDE_ACTIVE = booleanPreferencesKey("security_emergency_override_active")
        val EMERGENCY_OVERRIDE_EXPIRATION = longPreferencesKey("security_emergency_override_expiration")
        val AUTO_LOCK_SECONDS = intPreferencesKey("security_auto_lock_seconds")
        val REQUIRE_AUTH_PROTECTION = booleanPreferencesKey("security_require_auth_protection")
        val REQUIRE_AUTH_DATA_RESET = booleanPreferencesKey("security_require_auth_data_reset")
    }

    private val safeData: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val isPinSet: Flow<Boolean> = safeData
        .map { it[Keys.IS_PIN_SET] ?: false }
        .distinctUntilChanged()

    override val isBiometricEnabled: Flow<Boolean> = safeData
        .map { it[Keys.BIOMETRIC_ENABLED] ?: false }
        .distinctUntilChanged()

    override val failedAttemptCount: Flow<Int> = safeData
        .map { it[Keys.FAILED_ATTEMPT_COUNT] ?: 0 }
        .distinctUntilChanged()

    override val lockoutUntilTimestamp: Flow<Long> = safeData
        .map { it[Keys.LOCKOUT_UNTIL] ?: 0L }
        .distinctUntilChanged()

    override val isEmergencyOverrideActive: Flow<Boolean> = safeData
        .map { it[Keys.EMERGENCY_OVERRIDE_ACTIVE] ?: false }
        .distinctUntilChanged()

    override val emergencyOverrideExpiration: Flow<Long> = safeData
        .map { it[Keys.EMERGENCY_OVERRIDE_EXPIRATION] ?: 0L }
        .distinctUntilChanged()

    override val autoLockInactivitySeconds: Flow<Int> = safeData
        .map { it[Keys.AUTO_LOCK_SECONDS] ?: 120 }
        .distinctUntilChanged()

    override val requireAuthForProtection: Flow<Boolean> = safeData
        .map { it[Keys.REQUIRE_AUTH_PROTECTION] ?: true }
        .distinctUntilChanged()

    override val requireAuthForDataReset: Flow<Boolean> = safeData
        .map { it[Keys.REQUIRE_AUTH_DATA_RESET] ?: true }
        .distinctUntilChanged()

    override suspend fun getStoredPinCredentials(): Pair<String, String>? {
        return runCatching {
            val prefs = safeData.first()
            val salt = prefs[Keys.PIN_SALT]
            val hash = prefs[Keys.PIN_HASH]
            if (!salt.isNullOrBlank() && !hash.isNullOrBlank()) {
                Pair(salt, hash)
            } else {
                null
            }
        }.getOrNull()
    }

    override suspend fun savePin(saltHex: String, hashHex: String): Result<Unit> = runCatching {
        dataStore.edit { prefs ->
            prefs[Keys.PIN_SALT] = saltHex
            prefs[Keys.PIN_HASH] = hashHex
            prefs[Keys.IS_PIN_SET] = true
            prefs[Keys.FAILED_ATTEMPT_COUNT] = 0
            prefs[Keys.LOCKOUT_UNTIL] = 0L
        }
    }

    override suspend fun clearPin(): Result<Unit> = runCatching {
        dataStore.edit { prefs ->
            prefs.remove(Keys.PIN_SALT)
            prefs.remove(Keys.PIN_HASH)
            prefs[Keys.IS_PIN_SET] = false
            prefs[Keys.BIOMETRIC_ENABLED] = false
            prefs[Keys.FAILED_ATTEMPT_COUNT] = 0
            prefs[Keys.LOCKOUT_UNTIL] = 0L
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit> = runCatching {
        dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    override suspend fun recordFailedAttempt(newCount: Int, lockoutUntilTimestamp: Long): Result<Unit> = runCatching {
        dataStore.edit { prefs ->
            prefs[Keys.FAILED_ATTEMPT_COUNT] = newCount
            prefs[Keys.LOCKOUT_UNTIL] = lockoutUntilTimestamp
        }
    }

    override suspend fun resetFailedAttempts(): Result<Unit> = runCatching {
        dataStore.edit { prefs ->
            prefs[Keys.FAILED_ATTEMPT_COUNT] = 0
            prefs[Keys.LOCKOUT_UNTIL] = 0L
        }
    }

    override suspend fun setEmergencyOverride(active: Boolean, expirationTimestamp: Long): Result<Unit> = runCatching {
        dataStore.edit { prefs ->
            prefs[Keys.EMERGENCY_OVERRIDE_ACTIVE] = active
            prefs[Keys.EMERGENCY_OVERRIDE_EXPIRATION] = expirationTimestamp
        }
    }

    override suspend fun setAutoLockInactivitySeconds(seconds: Int): Result<Unit> = runCatching {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_LOCK_SECONDS] = seconds
        }
    }

    override suspend fun setRequireAuthForProtection(required: Boolean): Result<Unit> = runCatching {
        dataStore.edit { prefs ->
            prefs[Keys.REQUIRE_AUTH_PROTECTION] = required
        }
    }

    override suspend fun setRequireAuthForDataReset(required: Boolean): Result<Unit> = runCatching {
        dataStore.edit { prefs ->
            prefs[Keys.REQUIRE_AUTH_DATA_RESET] = required
        }
    }
}

