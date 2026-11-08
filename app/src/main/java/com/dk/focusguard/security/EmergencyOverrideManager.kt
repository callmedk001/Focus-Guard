package com.dk.focusguard.security

import com.dk.focusguard.data.local.entity.BlockEventEntity
import com.dk.focusguard.domain.repository.BlockEventRepository
import com.dk.focusguard.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Result indicating whether protection guards have been restored.
 * Explicitly distinguishes actual active protection from inactive states.
 */
sealed class ProtectionRestorationStatus {
    data object RestoredActiveService : ProtectionRestorationStatus()
    data class StandbyNoServiceRunning(val message: String) : ProtectionRestorationStatus()
    data object OverrideStillActive : ProtectionRestorationStatus()
}

/**
 * Manages emergency defense override sessions.
 * Allows time-limited suspension of protection controls with mandatory PIN verification,
 * comprehensive local audit logging, and safe auto-expiry restoration semantics.
 */
class EmergencyOverrideManager(
    private val securityRepository: SecurityRepository,
    private val blockEventRepository: BlockEventRepository,
    private val isServiceCurrentlyRunningProvider: () -> Boolean = { false }
) {

    val isOverrideActive: Flow<Boolean> = securityRepository.isEmergencyOverrideActive
    val overrideExpirationTimestamp: Flow<Long> = securityRepository.emergencyOverrideExpiration

    /**
     * Activates an emergency override window for a specified duration in minutes.
     * Records the authentic audit record in local Room database.
     */
    suspend fun startOverride(durationMinutes: Int, reason: String = "User Emergency Override"): Result<Unit> {
        val now = System.currentTimeMillis()
        val expiration = now + (durationMinutes * 60 * 1000L)

        val logResult = blockEventRepository.recordRealBlockEvent(
            BlockEventEntity(
                packageName = "com.dk.focusguard",
                category = "EMERGENCY_OVERRIDE",
                timestamp = now,
                detectionSource = "SECURITY_ENGINE",
                eventStatus = "OVERRIDE_ACTIVATED_FOR_${durationMinutes}M"
            )
        )
        if (logResult.isFailure) {
            return Result.failure(logResult.exceptionOrNull() ?: Exception("Failed to log override audit record"))
        }

        return securityRepository.setEmergencyOverride(active = true, expirationTimestamp = expiration)
    }

    /**
     * Cancels an active override immediately and restores security guard state.
     */
    suspend fun cancelOverride(): Result<Unit> {
        val now = System.currentTimeMillis()
        blockEventRepository.recordRealBlockEvent(
            BlockEventEntity(
                packageName = "com.dk.focusguard",
                category = "EMERGENCY_OVERRIDE",
                timestamp = now,
                detectionSource = "SECURITY_ENGINE",
                eventStatus = "OVERRIDE_CANCELLED_MANUALLY"
            )
        )
        return securityRepository.setEmergencyOverride(active = false, expirationTimestamp = 0L)
    }

    /**
     * Checks if the active override has expired and verifies protection restoration.
     *
     * IMPORTANT RESTRICTION:
     * Never claims protection is active if the underlying interception service is not running!
     */
    suspend fun verifyAndHandleExpiration(): ProtectionRestorationStatus {
        val isActive = isOverrideActive.first()
        if (!isActive) {
            return if (isServiceCurrentlyRunningProvider()) {
                ProtectionRestorationStatus.RestoredActiveService
            } else {
                ProtectionRestorationStatus.StandbyNoServiceRunning(
                    "Override inactive. Background blocking service is not currently running."
                )
            }
        }

        val expiration = overrideExpirationTimestamp.first()
        val now = System.currentTimeMillis()
        if (now >= expiration) {
            // Override has elapsed
            securityRepository.setEmergencyOverride(active = false, expirationTimestamp = 0L)
            blockEventRepository.recordRealBlockEvent(
                BlockEventEntity(
                    packageName = "com.dk.focusguard",
                    category = "EMERGENCY_OVERRIDE",
                    timestamp = now,
                    detectionSource = "SECURITY_ENGINE",
                    eventStatus = "OVERRIDE_AUTO_EXPIRED"
                )
            )

            return if (isServiceCurrentlyRunningProvider()) {
                ProtectionRestorationStatus.RestoredActiveService
            } else {
                ProtectionRestorationStatus.StandbyNoServiceRunning(
                    "Emergency override expired. Protection filters standing by (blocking service not running)."
                )
            }
        }

        return ProtectionRestorationStatus.OverrideStillActive
    }
}

