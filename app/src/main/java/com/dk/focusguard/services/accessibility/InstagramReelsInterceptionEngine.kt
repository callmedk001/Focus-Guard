package com.dk.focusguard.services.accessibility

import com.dk.focusguard.data.local.entity.BlockEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Outcome resulting from evaluating an accessibility event in Instagram.
 */
enum class InstagramInterceptionOutcome {
    /** Reels confidently detected; back action executed and authentic block logged */
    INTERRUPTED_SUCCESS,

    /** Reels detected and interruption attempted, but system accessibility action failed */
    INTERRUPTION_FAILED,

    /** Event arrived within active session cooldown window; duplicate execution suppressed */
    SUPPRESSED_COOLDOWN,

    /** Instagram Reels protection setting is disabled by the user */
    PROTECTION_DISABLED,

    /** Security Emergency Override is currently active; enforcement safely suspended */
    OVERRIDE_ACTIVE,

    /** Normal Instagram Feed, DMs, Stories, Profiles, or Explore active; allowed without interference */
    NORMAL_CONTENT_ALLOWED,

    /** UI layout is ambiguous or uncertain; protection rules mandate NO ACTION */
    UNCERTAIN_IGNORED,

    /** Accessibility node hierarchy was null or unavailable */
    DETECTION_UNAVAILABLE,

    /** Event belongs to a non-Instagram package */
    NON_INSTAGRAM_PACKAGE
}

/**
 * Audit decision metadata produced by InstagramReelsInterceptionEngine.
 */
data class InstagramInterceptionDecision(
    val outcome: InstagramInterceptionOutcome,
    val analysis: InstagramDetectionAnalysis? = null,
    val wasInterruptionAttempted: Boolean = false,
    val eventRecorded: Boolean = false,
    val message: String = ""
)

/**
 * Real-time interception engine for Instagram Reels.
 *
 * GUARANTEES:
 * 1. REAL INTERACTION: Uses Accessibility back action to return the user to safe Instagram content.
 * 2. NO INFINITE LOOPS: Suppresses repeated trigger events during active cooldown window.
 * 3. NO FAKE LOGS: Writes BlockEventEntity only for genuine events; marks FAILED if action fails.
 * 4. OVERRIDE & SETTINGS RESPECT: Respects emergency override and user toggles immediately.
 * 5. NORMAL CONTENT UNTOUCHED: Never interrupts Direct Messages, Stories, Profiles, or standard Feed.
 */
class InstagramReelsInterceptionEngine(
    private val classifier: InstagramReelsClassifier = InstagramReelsClassifier(),
    private val isReelsProtectionEnabled: () -> Boolean,
    private val isEmergencyOverrideActive: () -> Boolean,
    private val performBackAction: () -> Boolean,
    private val triggerOverlay: () -> Unit = {},
    private val logEvent: suspend (BlockEventEntity) -> Result<Long> = { Result.success(0L) },
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val cooldownWindowMillis: Long = 2500L
) {

    private var lastInterruptionTimestamp: Long = 0L
    private var isSessionInCooldown: Boolean = false

    /**
     * Evaluates incoming accessibility node structure and enforces discipline rules.
     */
    fun evaluateAndEnforce(
        rootNode: AccessibleNode?,
        packageName: String?
    ): InstagramInterceptionDecision {
        // 1. Verify Target Package
        if (packageName != TargetAppRegistry.PACKAGE_INSTAGRAM) {
            return InstagramInterceptionDecision(
                outcome = InstagramInterceptionOutcome.NON_INSTAGRAM_PACKAGE,
                message = "Package $packageName is not Instagram"
            )
        }

        // 2. Check User Protection Setting
        if (!isReelsProtectionEnabled()) {
            return InstagramInterceptionDecision(
                outcome = InstagramInterceptionOutcome.PROTECTION_DISABLED,
                message = "Instagram Reels protection is disabled by user"
            )
        }

        // 3. Check Emergency Override State
        if (isEmergencyOverrideActive()) {
            return InstagramInterceptionDecision(
                outcome = InstagramInterceptionOutcome.OVERRIDE_ACTIVE,
                message = "Emergency override is active; enforcement suspended"
            )
        }

        // 4. Run Minimal Multi-Signal Node Classifier
        val analysis = classifier.classifyNodeHierarchy(rootNode)

        return when (analysis.result) {
            InstagramClassificationResult.DETECTION_UNAVAILABLE -> {
                InstagramInterceptionDecision(
                    outcome = InstagramInterceptionOutcome.DETECTION_UNAVAILABLE,
                    analysis = analysis,
                    message = "Accessibility hierarchy unavailable"
                )
            }

            InstagramClassificationResult.NORMAL_INSTAGRAM -> {
                // User is in normal Instagram (Feed, DMs, Stories, etc.); reset session cooldown
                resetInterruptionSession()
                InstagramInterceptionDecision(
                    outcome = InstagramInterceptionOutcome.NORMAL_CONTENT_ALLOWED,
                    analysis = analysis,
                    message = "Normal Instagram content allowed without interference"
                )
            }

            InstagramClassificationResult.UNKNOWN -> {
                // Ambiguous signals: RULE: If detection is uncertain, do not block
                InstagramInterceptionDecision(
                    outcome = InstagramInterceptionOutcome.UNCERTAIN_IGNORED,
                    analysis = analysis,
                    message = "Detection uncertain; safe pass-through without blocking"
                )
            }

            InstagramClassificationResult.REELS_CONFIRMED -> {
                val now = System.currentTimeMillis()

                // Check Cooldown to Prevent Infinite Loops & Duplicate Events
                if (isSessionInCooldown && (now - lastInterruptionTimestamp) < cooldownWindowMillis) {
                    return InstagramInterceptionDecision(
                        outcome = InstagramInterceptionOutcome.SUPPRESSED_COOLDOWN,
                        analysis = analysis,
                        message = "Interruption event suppressed within cooldown window"
                    )
                }

                // Execute Interruption
                lastInterruptionTimestamp = now
                isSessionInCooldown = true

                val backSucceeded = performBackAction()

                if (backSucceeded) {
                    // Safe Interruption Succeeded: Launch Cyber Overlay & Log Genuine Event
                    triggerOverlay()

                    coroutineScope.launch {
                        logEvent(
                            BlockEventEntity(
                                packageName = TargetAppRegistry.PACKAGE_INSTAGRAM,
                                category = "INSTAGRAM_REELS",
                                timestamp = now,
                                detectionSource = "ACCESSIBILITY_INTERCEPTION",
                                eventStatus = "INTERCEPTED"
                            )
                        )
                    }

                    InstagramInterceptionDecision(
                        outcome = InstagramInterceptionOutcome.INTERRUPTED_SUCCESS,
                        analysis = analysis,
                        wasInterruptionAttempted = true,
                        eventRecorded = true,
                        message = "Reels session successfully interrupted via accessibility back action"
                    )
                } else {
                    // Action Failed: Log Failure, Never Claim False Success
                    coroutineScope.launch {
                        logEvent(
                            BlockEventEntity(
                                packageName = TargetAppRegistry.PACKAGE_INSTAGRAM,
                                category = "INSTAGRAM_REELS",
                                timestamp = now,
                                detectionSource = "ACCESSIBILITY_INTERCEPTION",
                                eventStatus = "FAILED"
                            )
                        )
                    }

                    InstagramInterceptionDecision(
                        outcome = InstagramInterceptionOutcome.INTERRUPTION_FAILED,
                        analysis = analysis,
                        wasInterruptionAttempted = true,
                        eventRecorded = true,
                        message = "Reels detected but accessibility back action returned false"
                    )
                }
            }
        }
    }

    /**
     * Resets active session cooldown state when user exits Reels or leaves Instagram.
     */
    fun resetInterruptionSession() {
        isSessionInCooldown = false
        lastInterruptionTimestamp = 0L
    }
}

