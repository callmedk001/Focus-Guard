package com.dk.focusguard.security

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal

/**
 * Device biometric hardware and enrollment capability status.
 */
enum class BiometricCapability {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

/**
 * Callback interface for biometric prompt events.
 */
interface BiometricAuthCallback {
    fun onSuccess()
    fun onError(errorCode: Int, errorMessage: String)
    fun onCancel()
}

/**
 * Integrates native Android Biometric APIs to provide secure hardware-backed
 * biometric authentication with guaranteed fallback to Master PIN.
 *
 * NOTE: Never captures, accesses, or stores raw biometric measurements.
 */
class BiometricAuthManager(private val context: Context) {

    private var cancellationSignal: CancellationSignal? = null

    /**
     * Inspects device hardware sensors and user enrollment status.
     */
    fun checkBiometricCapability(): BiometricCapability {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            return BiometricCapability.NO_HARDWARE
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
                ?: return BiometricCapability.UNAVAILABLE

            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NO_HARDWARE
                else -> BiometricCapability.UNAVAILABLE
            }
        } else {
            @Suppress("DEPRECATION")
            val fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
                ?: return BiometricCapability.NO_HARDWARE

            @Suppress("DEPRECATION")
            if (!fingerprintManager.isHardwareDetected) {
                BiometricCapability.NO_HARDWARE
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                BiometricCapability.NONE_ENROLLED
            } else {
                BiometricCapability.AVAILABLE
            }
        }
    }

    /**
     * Prompts the user with the native Android Biometric Dialog.
     * If the user selects the negative button, cancels, or fails, the callback falls back cleanly to PIN.
     */
    fun authenticate(
        activity: Activity,
        title: String = "Authorize Sensitive Action",
        subtitle: String = "Verify your biometric identity to proceed",
        negativeButtonText: String = "Use PIN",
        callback: BiometricAuthCallback
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cancellationSignal?.cancel()
            val signal = CancellationSignal()
            cancellationSignal = signal

            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton(negativeButtonText, activity.mainExecutor, DialogInterface.OnClickListener { _, _ ->
                    callback.onCancel()
                })
                .build()

            prompt.authenticate(
                signal,
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        callback.onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        // Error code 10 = USER_CANCELED, code 13 = NEGATIVE_BUTTON ("Use PIN")
                        if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                            errorCode == 13
                        ) {
                            callback.onCancel()
                        } else {
                            callback.onError(errorCode, errString?.toString() ?: "Biometric authentication failed")
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        callback.onError(-1, "Biometric unverified. Please try again or use PIN.")
                    }
                }
            )
        } else {
            // Devices on API 26-27 fall back directly to PIN
            callback.onCancel()
        }
    }

    /**
     * Cancels any active biometric prompt.
     */
    fun cancelAuthentication() {
        cancellationSignal?.cancel()
        cancellationSignal = null
    }
}

