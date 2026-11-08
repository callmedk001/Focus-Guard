package com.dk.focusguard

import com.dk.focusguard.data.local.entity.BlockEventEntity
import com.dk.focusguard.fakes.FakeAccessibleNode
import com.dk.focusguard.services.accessibility.InstagramInterceptionOutcome
import com.dk.focusguard.services.accessibility.InstagramReelsClassifier
import com.dk.focusguard.services.accessibility.InstagramReelsInterceptionEngine
import com.dk.focusguard.services.accessibility.TargetAppRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying Instagram Reels interception engine behavior.
 * Covers requirements 8-14: protection toggles, emergency override, cooldown suppression,
 * authentic event logging, and failure handling.
 */
class InstagramReelsInterceptionEngineTest {

    private var isReelsProtectionEnabled = true
    private var isEmergencyOverrideActive = false
    private var backActionReturnValue = true
    private var backActionCallCount = 0
    private var overlayCallCount = 0
    private val loggedEvents = mutableListOf<BlockEventEntity>()

    private lateinit var engine: InstagramReelsInterceptionEngine

    private val reelsRootNode = FakeAccessibleNode(
        viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
        children = listOf(
            FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/clips_video_container")
        )
    )

    private val dmRootNode = FakeAccessibleNode(
        viewIdResourceName = "com.instagram.android:id/direct_thread_layout",
        children = listOf(
            FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/message_list")
        )
    )

    private val normalFeedRootNode = FakeAccessibleNode(
        viewIdResourceName = "com.instagram.android:id/feed_recycler_view",
        children = listOf(
            FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/row_feed_photo_media")
        )
    )

    @Before
    fun setUp() {
        isReelsProtectionEnabled = true
        isEmergencyOverrideActive = false
        backActionReturnValue = true
        backActionCallCount = 0
        overlayCallCount = 0
        loggedEvents.clear()

        engine = InstagramReelsInterceptionEngine(
            classifier = InstagramReelsClassifier(),
            isReelsProtectionEnabled = { isReelsProtectionEnabled },
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
    fun test8_reelsProtectionDisabledDoesNotInterrupt() = runBlocking {
        // Scenario 8: Reels protection disabled by user
        isReelsProtectionEnabled = false

        val decision = engine.evaluateAndEnforce(reelsRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)

        assertEquals(InstagramInterceptionOutcome.PROTECTION_DISABLED, decision.outcome)
        assertEquals(0, backActionCallCount)
        assertEquals(0, overlayCallCount)
        assertEquals(0, loggedEvents.size)
    }

    @Test
    fun test11_reelsProtectionEnabledExecutesInterruptionAndLogsSuccess() = runBlocking {
        // Scenario 11: Reels protection enabled -> executes back action, displays overlay, logs INTERCEPTED
        isReelsProtectionEnabled = true

        val decision = engine.evaluateAndEnforce(reelsRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)

        assertEquals(InstagramInterceptionOutcome.INTERRUPTED_SUCCESS, decision.outcome)
        assertTrue(decision.wasInterruptionAttempted)
        assertTrue(decision.eventRecorded)
        assertEquals(1, backActionCallCount)
        assertEquals(1, overlayCallCount)

        assertEquals(1, loggedEvents.size)
        val event = loggedEvents.first()
        assertEquals(TargetAppRegistry.PACKAGE_INSTAGRAM, event.packageName)
        assertEquals("INSTAGRAM_REELS", event.category)
        assertEquals("INTERCEPTED", event.eventStatus)
        assertEquals("ACCESSIBILITY_INTERCEPTION", event.detectionSource)
    }

    @Test
    fun test9_emergencyOverrideActiveSuspendsInterruption() = runBlocking {
        // Scenario 9: Emergency override active -> safe pass-through without blocking
        isEmergencyOverrideActive = true

        val decision = engine.evaluateAndEnforce(reelsRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)

        assertEquals(InstagramInterceptionOutcome.OVERRIDE_ACTIVE, decision.outcome)
        assertEquals(0, backActionCallCount)
        assertEquals(0, overlayCallCount)
        assertEquals(0, loggedEvents.size)
    }

    @Test
    fun test13_duplicateEventSuppressionWithinCooldown() = runBlocking {
        // Scenario 13: Rapid repeated events from the same Reels session are debounced
        val firstDecision = engine.evaluateAndEnforce(reelsRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)
        assertEquals(InstagramInterceptionOutcome.INTERRUPTED_SUCCESS, firstDecision.outcome)
        assertEquals(1, backActionCallCount)
        assertEquals(1, overlayCallCount)

        // Immediate subsequent event within cooldown window
        val secondDecision = engine.evaluateAndEnforce(reelsRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)
        assertEquals(InstagramInterceptionOutcome.SUPPRESSED_COOLDOWN, secondDecision.outcome)
        assertEquals(1, backActionCallCount) // No second back action!
        assertEquals(1, overlayCallCount)    // No second overlay!

        assertEquals(1, loggedEvents.size) // Only 1 genuine event logged, no duplicates!
    }

    @Test
    fun test12_failedInterruptionRecordingNeverClaimsSuccess() = runBlocking {
        // Scenario 12: Back action returns false -> logged as FAILED, never INTERCEPTED
        backActionReturnValue = false

        val decision = engine.evaluateAndEnforce(reelsRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)

        assertEquals(InstagramInterceptionOutcome.INTERRUPTION_FAILED, decision.outcome)
        assertTrue(decision.wasInterruptionAttempted)
        assertEquals(1, backActionCallCount)
        assertEquals(0, overlayCallCount) // Overlay not shown when action failed

        assertEquals(1, loggedEvents.size)
        assertEquals("FAILED", loggedEvents.first().eventStatus)
    }

    @Test
    fun test14_noFakeBlockRecordsWhenNormalFeedOrDMs() = runBlocking {
        // Scenario 14: Zero fake block records when viewing normal feed, DMs, or uncertain layout
        val dmDecision = engine.evaluateAndEnforce(dmRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)
        assertEquals(InstagramInterceptionOutcome.NORMAL_CONTENT_ALLOWED, dmDecision.outcome)

        val feedDecision = engine.evaluateAndEnforce(normalFeedRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)
        assertEquals(InstagramInterceptionOutcome.NORMAL_CONTENT_ALLOWED, feedDecision.outcome)

        val uncertainNode = FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/random_unknown_view")
        val uncertainDecision = engine.evaluateAndEnforce(uncertainNode, TargetAppRegistry.PACKAGE_INSTAGRAM)
        assertEquals(InstagramInterceptionOutcome.UNCERTAIN_IGNORED, uncertainDecision.outcome)

        assertEquals(0, loggedEvents.size)
        assertEquals(0, backActionCallCount)
    }

    @Test
    fun testTransitionToNormalInstagramResetsInterruptionSession() = runBlocking {
        // Intercept first Reels session
        val first = engine.evaluateAndEnforce(reelsRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)
        assertEquals(InstagramInterceptionOutcome.INTERRUPTED_SUCCESS, first.outcome)

        // User returns to Direct Messages
        val dm = engine.evaluateAndEnforce(dmRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)
        assertEquals(InstagramInterceptionOutcome.NORMAL_CONTENT_ALLOWED, dm.outcome)

        // User navigates back to Reels: session was reset, so it must be immediately intercepted
        val second = engine.evaluateAndEnforce(reelsRootNode, TargetAppRegistry.PACKAGE_INSTAGRAM)
        assertEquals(InstagramInterceptionOutcome.INTERRUPTED_SUCCESS, second.outcome)
        assertEquals(2, backActionCallCount)
    }

    @Test
    fun testNonInstagramPackageIsIgnored() = runBlocking {
        val decision = engine.evaluateAndEnforce(reelsRootNode, "com.twitter.android")
        assertEquals(InstagramInterceptionOutcome.NON_INSTAGRAM_PACKAGE, decision.outcome)
        assertEquals(0, backActionCallCount)
        assertEquals(0, loggedEvents.size)
    }
}

