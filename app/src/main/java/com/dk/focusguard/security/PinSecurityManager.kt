package com.dk.focusguard.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Result of deriving a cryptographic hash from a user PIN.
 * Stores only the random salt and derived key, never the original PIN.
 */
data class PinHashResult(
    val saltHex: String,
    val hashHex: String
)

/**
 * Result of validating the format constraints of a PIN input.
 */
data class PinValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Cryptographic security manager providing industry-standard password hashing,
 * constant-time verification, and secure random salt generation for DK FOCUS GUARD.
 *
 * Technical Specifications:
 * - Algorithm: PBKDF2WithHmacSHA256
 * - Iterations: 65,536
 * - Key Length: 256 bits (32 bytes)
 * - Salt: 16 bytes (128 bits) via cryptographically strong SecureRandom
 * - Comparison: Constant-time comparison via MessageDigest.isEqual to prevent timing attacks
 * - Memory Safety: Internal character array zeroization
 */
object PinSecurityManager {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 65_536
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    /**
     * Validates that the provided PIN meets security format constraints:
     * - Must be between 4 and 6 characters in length.
     * - Must contain only numeric digits (0-9).
     */
    fun validatePinFormat(pin: String): PinValidationResult {
        if (pin.length < 4 || pin.length > 6) {
            return PinValidationResult(
                isValid = false,
                errorMessage = "PIN must be between 4 and 6 digits"
            )
        }
        if (!pin.all { it.isDigit() }) {
            return PinValidationResult(
                isValid = false,
                errorMessage = "PIN must contain only numeric digits"
            )
        }
        return PinValidationResult(isValid = true)
    }

    /**
     * Generates a cryptographically strong random salt.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Hashes a PIN using PBKDF2 with HMAC-SHA256 and a random salt.
     * Never stores or logs the plaintext PIN.
     *
     * @param pin The 4-6 digit numeric PIN.
     * @param salt Optional predefined salt for deterministic testing or regeneration.
     * @return [PinHashResult] containing hex-encoded salt and derived hash.
     */
    fun hashPin(pin: String, salt: ByteArray = generateSalt()): PinHashResult {
        val validation = validatePinFormat(pin)
        require(validation.isValid) { validation.errorMessage ?: "Invalid PIN format" }

        val pinChars = pin.toCharArray()
        val spec = PBEKeySpec(pinChars, salt, ITERATIONS, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val hash = factory.generateSecret(spec).encoded
            return PinHashResult(
                saltHex = salt.toHex(),
                hashHex = hash.toHex()
            )
        } finally {
            spec.clearPassword()
            pinChars.fill('0') // Zeroize sensitive memory buffer
        }
    }

    /**
     * Verifies an entered candidate PIN against the stored salt and hash using
     * constant-time byte comparison to eliminate side-channel timing attack vectors.
     *
     * @param candidatePin The candidate PIN entered by the user.
     * @param storedSaltHex The hex-encoded salt previously stored in secure preferences.
     * @param storedHashHex The hex-encoded hash previously stored in secure preferences.
     * @return True if candidate matches the stored hash, false otherwise.
     */
    fun verifyPin(candidatePin: String, storedSaltHex: String, storedHashHex: String): Boolean {
        if (!validatePinFormat(candidatePin).isValid) {
            return false
        }
        if (storedSaltHex.isBlank() || storedHashHex.isBlank()) {
            return false
        }

        val salt = runCatching { storedSaltHex.hexToBytes() }.getOrNull() ?: return false
        val expectedHash = runCatching { storedHashHex.hexToBytes() }.getOrNull() ?: return false

        val pinChars = candidatePin.toCharArray()
        val spec = PBEKeySpec(pinChars, salt, ITERATIONS, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val derivedHash = factory.generateSecret(spec).encoded
            // Constant-time comparison ensures timing attack resistance
            return MessageDigest.isEqual(derivedHash, expectedHash)
        } catch (_: Exception) {
            return false
        } finally {
            spec.clearPassword()
            pinChars.fill('0')
        }
    }

    private fun ByteArray.toHex(): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(size * 2)
        for (b in this) {
            val byteVal = b.toInt() and 0xFF
            result.append(hexChars[byteVal shr 4])
            result.append(hexChars[byteVal and 0x0F])
        }
        return result.toString()
    }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "Hex string length must be even" }
        val result = ByteArray(length / 2)
        for (i in indices step 2) {
            val firstDigit = Character.digit(this[i], 16)
            val secondDigit = Character.digit(this[i + 1], 16)
            require(firstDigit != -1 && secondDigit != -1) { "Invalid hexadecimal character" }
            result[i / 2] = ((firstDigit shl 4) + secondDigit).toByte()
        }
        return result
    }
}

