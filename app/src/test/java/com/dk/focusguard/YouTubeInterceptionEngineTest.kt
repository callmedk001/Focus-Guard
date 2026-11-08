package com.dk.focusguard

import com.dk.focusguard.data.local.entity.BlockEventEntity
import com.dk.focusguard.fakes.FakeAccessibleNode
import com.dk.focusguard.services.accessibility.InterceptionOutcome
import com.dk.focusguard.services.accessibility.TargetAppRegistry
import com.dk.focusguard.services.accessibility.YouTubeInterceptionEngine
import com.dk.focusguard.services.accessibility.YouTubeShortsClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying YouTube Shorts interception engine behavior.
 * Covers requirements 5-12: protection toggles, emergency override, cooldown suppression,
 * authentic event logging, and failure handling.
 */
class YouTubeInterceptionEngineTest {

    private var isShortsProtectionEnabled = true
    private var isEmergencyOverrideActive = false
    private var backActionReturnValue = true
    private var backActionCallCount = 0
    private var overlayCallCount = 0
    private val loggedEvents = mutableListOf<BlockEventEntity>()

    private lateinit var engine: YouTubeInterceptionEngine

    private val shortsRootNode = FakeAccessibleNode(
        viewIdResourceName = "com.google.android.youtube:id/reel_recycler",
        children = listOf(
            FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/reel_player_view")
        )
    )

    private val normalWatchRootNode = FakeAccessibleNode(
        viewIdResourceName = "com.google.android.youtube:id/watch_player",
        children = listOf(
            FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/player_view")
        )
    )

    @Before
    fun setUp() {
        isShortsProtectionEnabled = true
        isEmergencyOverrideActive = false
        backActionReturnValue = true
        backActionCallCount = 0
        overlayCallCount = 0
        loggedEvents.clear()

        engine = YouTubeInterceptionEngine(
            classifier = YouTubeShortsClassifier(),
            isShortsProtectionEnabled = { isShortsProtectionEnabled },
            isEmergencyOverrideActive = { isEmergencyOverrideActive },
            performBackAction = {
                backActionCallCount++
                backActionReturnValue
            },
            triggerOverlay = {
                overlayCallCount++
            },
            logEvent = { event ->
                loggedEvents.add(event)
                Result.success(1L)
            },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            cooldownWindowMillis = 2000L
        )
    }

    @Test
    fun testShortsProtectionDisabledDoesNotInterrupt() = runBlocking {
        // Scenario 5: Shorts protection disabled by user
        isShortsProtectionEnabled = false

        val decision = engine.evaluateAndEnforce(shortsRootNode, TargetAppRegistry.PACKAGE_YOUTUBE)

        assertEquals(InterceptionOutcome.PROTECTION_DISABLED, decision.outcome)
        assertEquals(0, backActionCallCount)
        assertEquals(0, overlayCallCount)
        assertEquals(0, loggedEvents.size)
    }

    @Test
    fun testShortsProtectionEnabledExecutesInterruptionAndLogsSuccess() = runBlocking {
        // Scenario 6 & 10: Shorts protection enabled -> executes back action, displays overlay, logs INTERCEPTED
        isShortsProtectionEnabled = true

        val decision = engine.evaluateAndEnforce(shortsRootNode, TargetAppRegistry.PACKAGE_YOUTUBE)

        assertEquals(InterceptionOutcome.INTERRUPTED_SUCCESS, decision.outcome)
        assertTrue(decision.wasInterruptionAttempted)
        assertTrue(decision.eventRecorded)
        assertEquals(1, backActionCallCount)
        assertEquals(1, overlayCallCount)

        assertEquals(1, loggedEvents.size)
        val event = loggedEvents.first()
        assertEquals(TargetAppRegistry.PACKAGE_YOUTUBE, event.packageName)
        assertEquals("YOUTUBE_SHORTS", event.category)
        assertEquals("INTERCEPTED", event.eventStatus)
        assertEquals("ACCESSIBILITY_INTERCEPTION", event.detectionSource)
    }

    @Test
    fun testEmergencyOverrideActiveSuspendsInterruption() = runBlocking {
        // Scenario 7: Emergency override active -> safe pass-through without blocking
        isEmergencyOverrideActive = true

        val decision = engine.evaluateAndEnforce(shortsRootNode, TargetAppRegistry.PACKAGE_YOUTUBE)

        assertEquals(InterceptionOutcome.OVERRIDE_ACTIVE, decision.outcome)
        assertEquals(0, backActionCallCount)
        assertEquals(0, overlayCallCount)
        assertEquals(0, loggedEvents.size)
    }

    @Test
    fun testDuplicateEventSuppressionWithinCooldown() = runBlocking {
        // Scenario 9: Rapid repeated events from the same Shorts session are debounced
        val firstDecision = engine.evaluateAndEnforce(shortsRootNode, TargetAppRegistry.PACKAGE_YOUTUBE)
        assertEquals(InterceptionOutcome.INTERRUPTED_SUCCESS, firstDecision.outcome)
        assertEquals(1, backActionCallCount)
        assertEquals(1, overlayCallCount)

        // Immediate subsequent event within cooldown window
        val secondDecision = engine.evaluateAndEnforce(shortsRootNode, TargetAppRegistry.PACKAGE_YOUTUBE)
        assertEquals(InterceptionOutcome.SUPPRESSED_COOLDOWN, secondDecision.outcome)
        assertEquals(1, backActionCallCount) // No second back action!
        assertEquals(1, overlayCallCount)    // No second overlay!

        assertEquals(1, loggedEvents.size) // Only 1 genuine event logged, no duplicates!
    }

    @Test
    fun testFailedInterruptionRecordingNeverClaimsSuccess() = runBlocking {
        // Scenario 11: Back action returns false -> logged as FAILED, never INTERCEPTED
        backActionReturnValue = false

        val decision = engine.evaluateAndEnforce(shortsRootNode, TargetAppRegistry.PACKAGE_YOUTUBE)

        assertEquals(InterceptionOutcome.INTERRUPTION_FAILED, decision.outcome)
        assertTrue(decision.wasInterruptionAttempted)
        assertEquals(1, backActionCallCount)
        assertEquals(0, overlayCallCount) // Overlay not shown when action failed

        assertEquals(1, loggedEvents.size)
        assertEquals("FAILED", loggedEvents.first().eventStatus)
    }

    @Test
    fun testNoFakeBlockRecordsWhenNormalVideoOrUncertain() = runBlocking {
        // Scenario 12: Zero fake block records when viewing normal video or uncertain layout
        val normalDecision = engine.evaluateAndEnforce(normalWatchRootNode, TargetAppRegistry.PACKAGE_YOUTUBE)
        assertEquals(InterceptionOutcome.NORMAL_CONTENT_ALLOWED, normalDecision.outcome)

        val uncertainNode = FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/random_unknown_view")
        val uncertainDecision = engine.evaluateAndEnforce(uncertainNode, TargetAppRegistry.PACKAGE_YOUTUBE)
        assertEquals(InterceptionOutcome.UNCERTAIN_IGNORED, uncertainDecision.outcome)

        assertEquals(0, loggedEvents.size)
        assertEquals(0, backActionCallCount)
    }

    @Test
    fun testTransitionToNormalYouTubeResetsInterruptionSession() = runBlocking {
        // Intercept first Shorts session
        val first = engine.evaluateAndEnforce(shortsRootNode, TargetAppRegistry.PACKAGE_YOUTUBE)
        assertEquals(InterceptionOutcome.INTERRUPTED_SUCCESS, first.outcome)

        // User returns to normal YouTube video
        val normal = engine.evaluateAndEnforce(normalWatchRootNode, TargetAppRegistry.PACKAGE_YOUTUBE)
        assertEquals(InterceptionOutcome.NORMAL_CONTENT_ALLOWED, normal.outcome)

        // User attempts Shorts again: session should be reset and immediately intercepted
        val second = engine.evaluateAndEnforce(shortsRootNode, TargetAppRegistry.PACKAGE_YOUTUBE)
        assertEquals(InterceptionOutcome.INTERRUPTED_SUCCESS, second.outcome)
        assertEquals(2, backActionCallCount)
    }

    @Test
    fun testNonYouTubePackageIsIgnored() = runBlocking {
        val decision = engine.evaluateAndEnforce(shortsRootNode, "com.whatsapp")
        assertEquals(InterceptionOutcome.NON_YOUTUBE_PACKAGE, decision.outcome)
        assertEquals(0, backActionCallCount)
        assertEquals(0, loggedEvents.size)
    }
}
