package com.dk.focusguard

import com.dk.focusguard.data.repository.BlockEventRepositoryImpl
import com.dk.focusguard.fakes.FakeBlockEventDao
import com.dk.focusguard.fakes.FakeSecurityRepository
import com.dk.focusguard.security.EmergencyOverrideManager
import com.dk.focusguard.security.ProtectionRestorationStatus
import com.dk.focusguard.security.SecurityLockManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests rate limiting, lockout persistence, authentication session timeouts,
 * security preference storage, emergency override expiry, and authorization resets.
 */
class SecurityLockAndPolicyTest {

    private lateinit var lockManager: SecurityLockManager
    private lateinit var fakeSecurityRepository: FakeSecurityRepository
    private lateinit var fakeBlockEventDao: FakeBlockEventDao
    private lateinit var blockEventRepository: BlockEventRepositoryImpl
    private lateinit var emergencyOverrideManager: EmergencyOverrideManager

    @Before
    fun setUp() {
        lockManager = SecurityLockManager()
        fakeSecurityRepository = FakeSecurityRepository()
        fakeBlockEventDao = FakeBlockEventDao()
        blockEventRepository = BlockEventRepositoryImpl(dao = fakeBlockEventDao)
        emergencyOverrideManager = EmergencyOverrideManager(
            securityRepository = fakeSecurityRepository,
            blockEventRepository = blockEventRepository,
            isServiceCurrentlyRunningProvider = { false } // No service running in unit test
        )
    }

    @Test
    fun test7_rateLimiting_calculatesPenaltiesProgressively() {
        // Attempts 1 to 4 -> No lockout
        assertEquals(0, lockManager.calculateLockoutSeconds(1))
        assertEquals(0, lockManager.calculateLockoutSeconds(4))

        // 5th attempt -> 30 seconds
        assertEquals(30, lockManager.calculateLockoutSeconds(5))

        // 6th attempt -> 45 seconds
        assertEquals(45, lockManager.calculateLockoutSeconds(6))

        // 7th attempt -> 60 seconds
        assertEquals(70 - 10, lockManager.calculateLockoutSeconds(7))

        // 8+ attempts -> 300 seconds (5 minutes)
        assertEquals(300, lockManager.calculateLockoutSeconds(8))
        assertEquals(300, lockManager.calculateLockoutSeconds(15))
    }

    @Test
    fun test8_lockoutPersistence_tracksLockoutStateAndRemainingSeconds() = runBlocking {
        val now = 1000000L
        val lockoutUntil = now + (30 * 1000L) // 30 seconds penalty

        fakeSecurityRepository.recordFailedAttempt(newCount = 5, lockoutUntilTimestamp = lockoutUntil)

        val storedLockout = fakeSecurityRepository.lockoutUntilTimestamp.first()
        val storedAttempts = fakeSecurityRepository.failedAttemptCount.first()

        assertEquals(lockoutUntil, storedLockout)
        assertEquals(5, storedAttempts)

        // At time = now + 10s: still locked out, 20s remaining
        assertTrue(lockManager.isLockedOut(storedLockout, currentTimeMillis = now + 10000L))
        assertEquals(20L, lockManager.getRemainingLockoutSeconds(storedLockout, currentTimeMillis = now + 10000L))

        // At time = now + 35s: lockout expired
        assertFalse(lockManager.isLockedOut(storedLockout, currentTimeMillis = now + 35000L))
        assertEquals(0L, lockManager.getRemainingLockoutSeconds(storedLockout, currentTimeMillis = now + 35000L))
    }

    @Test
    fun test9_authenticationSessionTimeout_authorizesAndExpiresGracefully() {
        // Initially unauthorized
        assertFalse(lockManager.isSessionAuthorized.value)
        assertFalse(lockManager.isSessionValid())

        // Authorize with short session (e.g. 50ms)
        lockManager.authorizeSession(durationMillis = 50L)
        assertTrue(lockManager.isSessionAuthorized.value)
        assertTrue(lockManager.isSessionValid())

        // Simulate app backgrounding
        lockManager.onAppBackgrounded()
        assertFalse("App backgrounding must immediately revoke authorization", lockManager.isSessionAuthorized.value)
        assertFalse(lockManager.isSessionValid())
    }

    @Test
    fun test10_securityPreferencePersistence_storesConfiguredPreferences() = runBlocking {
        fakeSecurityRepository.setBiometricEnabled(true)
        fakeSecurityRepository.setAutoLockInactivitySeconds(300)
        fakeSecurityRepository.setRequireAuthForProtection(false)
        fakeSecurityRepository.setRequireAuthForDataReset(true)

        assertTrue(fakeSecurityRepository.isBiometricEnabled.first())
        assertEquals(300, fakeSecurityRepository.autoLockInactivitySeconds.first())
        assertFalse(fakeSecurityRepository.requireAuthForProtection.first())
        assertTrue(fakeSecurityRepository.requireAuthForDataReset.first())
    }

    @Test
    fun test11_emergencyOverrideExpiryLogic_managesLifecycleSafely() = runBlocking {
        assertFalse(emergencyOverrideManager.isOverrideActive.first())

        // Start 15-minute override
        val result = emergencyOverrideManager.startOverride(durationMinutes = 15)
        assertTrue(result.isSuccess)
        assertTrue(emergencyOverrideManager.isOverrideActive.first())

        // Verify authentic audit record was logged
        val auditEvents = blockEventRepository.getAllRealEvents().first()
        assertEquals(1, auditEvents.size)
        assertEquals("com.dk.focusguard", auditEvents[0].packageName)
        assertEquals("EMERGENCY_OVERRIDE", auditEvents[0].category)

        // Cancel override manually
        val cancelResult = emergencyOverrideManager.cancelOverride()
        assertTrue(cancelResult.isSuccess)
        assertFalse(emergencyOverrideManager.isOverrideActive.first())

        // Verify expiration check when service is not running does not falsely claim active protection
        val status = emergencyOverrideManager.verifyAndHandleExpiration()
        assertTrue(
            "Must report StandbyNoServiceRunning when no service is active",
            status is ProtectionRestorationStatus.StandbyNoServiceRunning
        )
    }

    @Test
    fun test12_resetAuthorization_clearsCredentialsAndSecurityState() = runBlocking {
        // Setup credentials
        fakeSecurityRepository.savePin(saltHex = "abcdef123456", hashHex = "fedcba654321")
        fakeSecurityRepository.setBiometricEnabled(true)
        fakeSecurityRepository.recordFailedAttempt(3, 5000L)

        assertTrue(fakeSecurityRepository.isPinSet.first())
        assertTrue(fakeSecurityRepository.isBiometricEnabled.first())

        // Execute clear/reset
        val clearResult = fakeSecurityRepository.clearPin()
        assertTrue(clearResult.isSuccess)

        assertFalse("PIN set flag must be false after clear", fakeSecurityRepository.isPinSet.first())
        assertFalse("Biometrics must be disabled after clear", fakeSecurityRepository.isBiometricEnabled.first())
        assertEquals(0, fakeSecurityRepository.failedAttemptCount.first())
        assertEquals(0L, fakeSecurityRepository.lockoutUntilTimestamp.first())
        assertNull(fakeSecurityRepository.getStoredPinCredentials())
    }
}

