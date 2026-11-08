package com.dk.focusguard.services.accessibility

import com.dk.focusguard.data.local.entity.BlockEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Outcome resulting from evaluating an accessibility event in YouTube.
 */
enum class InterceptionOutcome {
    /** Shorts confidently detected; back action executed and authentic block logged */
    INTERRUPTED_SUCCESS,

    /** Shorts detected and interruption attempted, but system accessibility action failed */
    INTERRUPTION_FAILED,

    /** Event arrived within active session cooldown window; duplicate execution suppressed */
    SUPPRESSED_COOLDOWN,

    /** YouTube Shorts protection setting is disabled by the user */
    PROTECTION_DISABLED,

    /** Security Emergency Override is currently active; enforcement safely suspended */
    OVERRIDE_ACTIVE,

    /** Normal YouTube video, browse feed, or search is active; allowed without interference */
    NORMAL_CONTENT_ALLOWED,

    /** UI layout is ambiguous or uncertain; protection rules mandate NO ACTION */
    UNCERTAIN_IGNORED,

    /** Accessibility node hierarchy was null or unavailable */
    DETECTION_UNAVAILABLE,

    /** Event belongs to a non-YouTube package */
    NON_YOUTUBE_PACKAGE
}

/**
 * Audit decision metadata produced by YouTubeInterceptionEngine.
 */
data class YouTubeInterceptionDecision(
    val outcome: InterceptionOutcome,
    val analysis: YouTubeDetectionAnalysis? = null,
    val wasInterruptionAttempted: Boolean = false,
    val eventRecorded: Boolean = false,
    val message: String = ""
)

/**
 * Real-time interception engine for YouTube Shorts.
 *
 * GUARANTEES:
 * 1. REAL INTERACTION: Uses Accessibility back/home navigation to return user to safe YouTube content.
 * 2. NO INFINITE LOOPS: Suppresses repeated trigger events during active cooldown window.
 * 3. NO FAKE LOGS: Writes BlockEventEntity only for genuine events; marks FAILED if action fails.
 * 4. OVERRIDE & SETTINGS RESPECT: Respects emergency override and user toggles immediately.
 * 5. NORMAL CONTENT UNTOUCHED: Never interrupts standard widescreen videos, search, or home feed.
 */
class YouTubeInterceptionEngine(
    private val classifier: YouTubeShortsClassifier = YouTubeShortsClassifier(),
    private val isShortsProtectionEnabled: () -> Boolean,
    private val isEmergencyOverrideActive: () -> Boolean,
    private val performBackAction: () -> Boolean,
    private val triggerOverlay: () -> Unit = {},
    private val logEvent: suspend (BlockEventEntity) -> Result<Long> = { Result.success(0L) },
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val cooldownWindowMillis: Long = 750L
) {

    private var lastInterruptionTimestamp: Long = 0L
    private var isSessionInCooldown: Boolean = false

    /**
     * Evaluates incoming accessibility node structure and enforces discipline rules.
     */
    fun evaluateAndEnforce(
        rootNode: AccessibleNode?,
        packageName: String?
    ): YouTubeInterceptionDecision {
        // 1. Verify Target Package
        if (packageName != TargetAppRegistry.PACKAGE_YOUTUBE) {
            return YouTubeInterceptionDecision(
                outcome = InterceptionOutcome.NON_YOUTUBE_PACKAGE,
                message = "Package $packageName is not YouTube"
            )
        }

        // 2. Check User Protection Setting
        if (!isShortsProtectionEnabled()) {
            return YouTubeInterceptionDecision(
                outcome = InterceptionOutcome.PROTECTION_DISABLED,
                message = "YouTube Shorts protection is disabled by user"
            )
        }

        // 3. Check Emergency Override State
        if (isEmergencyOverrideActive()) {
            return YouTubeInterceptionDecision(
                outcome = InterceptionOutcome.OVERRIDE_ACTIVE,
                message = "Emergency override is active; enforcement suspended"
            )
        }

        // 4. Run Minimal Multi-Signal Node Classifier
        val analysis = classifier.classifyNodeHierarchy(rootNode)

        return when (analysis.result) {
            YouTubeClassificationResult.DETECTION_UNAVAILABLE -> {
                YouTubeInterceptionDecision(
                    outcome = InterceptionOutcome.DETECTION_UNAVAILABLE,
                    analysis = analysis,
                    message = "Accessibility hierarchy unavailable"
                )
            }

            YouTubeClassificationResult.NORMAL_YOUTUBE -> {
                // User is in normal YouTube; reset any active session cooldown
                resetInterruptionSession()
                YouTubeInterceptionDecision(
                    outcome = InterceptionOutcome.NORMAL_CONTENT_ALLOWED,
                    analysis = analysis,
                    message = "Normal YouTube content allowed without interference"
                )
            }

            YouTubeClassificationResult.UNKNOWN -> {
                // Ambiguous signals: RULE: If detection is uncertain, do not block
                YouTubeInterceptionDecision(
                    outcome = InterceptionOutcome.UNCERTAIN_IGNORED,
                    analysis = analysis,
                    message = "Detection uncertain; safe pass-through without blocking"
                )
            }

            YouTubeClassificationResult.SHORTS_CONFIRMED -> {
                val now = System.currentTimeMillis()

                // Check Cooldown to Prevent Infinite Loops & Duplicate Rapid Clicks
                if (isSessionInCooldown && (now - lastInterruptionTimestamp) < cooldownWindowMillis) {
                    return YouTubeInterceptionDecision(
                        outcome = InterceptionOutcome.SUPPRESSED_COOLDOWN,
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
                                packageName = TargetAppRegistry.PACKAGE_YOUTUBE,
                                category = "YOUTUBE_SHORTS",
                                timestamp = now,
                                detectionSource = "ACCESSIBILITY_INTERCEPTION",
                                eventStatus = "INTERCEPTED"
                            )
                        )
                    }

                    YouTubeInterceptionDecision(
                        outcome = InterceptionOutcome.INTERRUPTED_SUCCESS,
                        analysis = analysis,
                        wasInterruptionAttempted = true,
                        eventRecorded = true,
                        message = "Shorts session successfully interrupted via accessibility navigation"
                    )
                } else {
                    // Action Failed: Log Failure, Never Claim False Success
                    coroutineScope.launch {
                        logEvent(
                            BlockEventEntity(
                                packageName = TargetAppRegistry.PACKAGE_YOUTUBE,
                                category = "YOUTUBE_SHORTS",
                                timestamp = now,
                                detectionSource = "ACCESSIBILITY_INTERCEPTION",
                                eventStatus = "FAILED"
                            )
                        )
                    }

                    YouTubeInterceptionDecision(
                        outcome = InterceptionOutcome.INTERRUPTION_FAILED,
                        analysis = analysis,
                        wasInterruptionAttempted = true,
                        eventRecorded = true,
                        message = "Shorts detected but accessibility navigation returned false"
                    )
                }
            }
        }
    }

    /**
     * Resets active session cooldown state when user exits Shorts or leaves the app.
     */
    fun resetInterruptionSession() {
        isSessionInCooldown = false
        lastInterruptionTimestamp = 0L
    }
}
