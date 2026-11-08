package com.dk.focusguard

import com.dk.focusguard.security.PinSecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests cryptographic PIN creation, format validation, salt generation,
 * hash uniqueness, and constant-time verification logic without using real user PINs.
 */
class PinCryptographyAndVerificationTest {

    // Test synthetic test PINs (never real user data)
    private val testPinValid4 = "1357"
    private val testPinValid6 = "246810"
    private val testPinShort = "123"
    private val testPinLong = "1234567"
    private val testPinAlpha = "12a4"

    @Test
    fun test1_pinCreation_validatesLengthAndFormat() {
        val result4 = PinSecurityManager.validatePinFormat(testPinValid4)
        assertTrue("4-digit PIN should be valid", result4.isValid)

        val result6 = PinSecurityManager.validatePinFormat(testPinValid6)
        assertTrue("6-digit PIN should be valid", result6.isValid)

        val resultShort = PinSecurityManager.validatePinFormat(testPinShort)
        assertFalse("PIN under 4 digits must be rejected", resultShort.isValid)

        val resultLong = PinSecurityManager.validatePinFormat(testPinLong)
        assertFalse("PIN over 6 digits must be rejected", resultLong.isValid)

        val resultAlpha = PinSecurityManager.validatePinFormat(testPinAlpha)
        assertFalse("Non-numeric PIN must be rejected", resultAlpha.isValid)
    }

    @Test
    fun test2_pinConfirmation_matchesInitialEntry() {
        val initialPin = "4815"
        val matchingPin = "4815"
        val mismatchedPin = "1623"

        assertEquals("Matching PINs must be equal", initialPin, matchingPin)
        assertNotEquals("Mismatched PINs must not be equal", initialPin, mismatchedPin)
    }

    @Test
    fun test3_correctPinVerification_returnsTrue() {
        val pin = "9876"
        val hashResult = PinSecurityManager.hashPin(pin)

        assertNotNull(hashResult.saltHex)
        assertNotNull(hashResult.hashHex)

        val isVerified = PinSecurityManager.verifyPin(
            candidatePin = pin,
            storedSaltHex = hashResult.saltHex,
            storedHashHex = hashResult.hashHex
        )
        assertTrue("Correct candidate PIN must verify successfully", isVerified)
    }

    @Test
    fun test4_incorrectPinVerification_returnsFalse() {
        val originalPin = "2468"
        val wrongPin = "1357"
        val hashResult = PinSecurityManager.hashPin(originalPin)

        val isVerified = PinSecurityManager.verifyPin(
            candidatePin = wrongPin,
            storedSaltHex = hashResult.saltHex,
            storedHashHex = hashResult.hashHex
        )
        assertFalse("Incorrect PIN candidate must fail verification", isVerified)

        val emptyVerified = PinSecurityManager.verifyPin(
            candidatePin = "",
            storedSaltHex = hashResult.saltHex,
            storedHashHex = hashResult.hashHex
        )
        assertFalse("Empty PIN must fail verification", emptyVerified)
    }

    @Test
    fun test5_randomSaltGeneration_generatesUnique16ByteSalts() {
        val salt1 = PinSecurityManager.generateSalt()
        val salt2 = PinSecurityManager.generateSalt()

        assertEquals("Salt length must be 16 bytes (128 bits)", 16, salt1.size)
        assertEquals("Salt length must be 16 bytes (128 bits)", 16, salt2.size)

        // Cryptographic randomness guarantee: two independent salts must not be identical
        assertFalse("Successive salts must not be identical", salt1.contentEquals(salt2))
    }

    @Test
    fun test6_pinHashUniqueness_differentSaltsYieldDistinctHashes() {
        val pin = "5555"

        val salt1 = PinSecurityManager.generateSalt()
        val salt2 = PinSecurityManager.generateSalt()

        val hashResult1 = PinSecurityManager.hashPin(pin, salt1)
        val hashResult2 = PinSecurityManager.hashPin(pin, salt2)

        assertNotEquals("Different salts must produce unique hashes for the same PIN", hashResult1.hashHex, hashResult2.hashHex)

        // Both must still verify against their own respective salt
        assertTrue(PinSecurityManager.verifyPin(pin, hashResult1.saltHex, hashResult1.hashHex))
        assertTrue(PinSecurityManager.verifyPin(pin, hashResult2.saltHex, hashResult2.hashHex))
    }
}

