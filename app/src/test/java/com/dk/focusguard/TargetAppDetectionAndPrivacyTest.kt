package com.dk.focusguard

import com.dk.focusguard.fakes.FakeAccessibleNode
import com.dk.focusguard.services.accessibility.AccessibilityEventProcessor
import com.dk.focusguard.services.accessibility.AccessibilityNodeInspector
import com.dk.focusguard.services.accessibility.TargetAppRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for:
 * 1. Target application package identification.
 * 2. Unknown package rejection.
 * 3. Null and unexpected accessibility events.
 * 6. Allowlisted package filtering.
 * 7. Accessibility event processor behavior.
 * 8. Node inspection privacy restrictions.
 */
class TargetAppDetectionAndPrivacyTest {

    private lateinit var nodeInspector: AccessibilityNodeInspector
    private lateinit var eventProcessor: AccessibilityEventProcessor
    private var lastDetectedPackage: String? = null
    private var lastDetectedAppName: String? = null

    @Before
    fun setUp() {
        nodeInspector = AccessibilityNodeInspector()
        eventProcessor = AccessibilityEventProcessor(
            nodeInspector = nodeInspector,
            throttleIntervalMillis = 100L,
            onTargetAppDetected = { pkg, name, _ ->
                lastDetectedPackage = pkg
                lastDetectedAppName = name
            }
        )
        lastDetectedPackage = null
        lastDetectedAppName = null
    }

    @Test
    fun test1_targetApplicationPackageIdentification_identifiesRegisteredApps() {
        assertTrue(
            "YouTube must be identified as target app",
            TargetAppRegistry.isTargetApp("com.google.android.youtube")
        )
        assertTrue(
            "Instagram must be identified as target app",
            TargetAppRegistry.isTargetApp("com.instagram.android")
        )
        assertFalse(
            "TikTok must be strictly excluded from target apps",
            TargetAppRegistry.isTargetApp("com.zhiliaoapp.musically")
        )

        val ytInfo = TargetAppRegistry.getTargetAppInfo("com.google.android.youtube")
        assertNotNull(ytInfo)
        assertEquals("YouTube", ytInfo?.displayName)

        val igInfo = TargetAppRegistry.getTargetAppInfo("com.instagram.android")
        assertNotNull(igInfo)
        assertEquals("Instagram", igInfo?.displayName)

        val ttInfo = TargetAppRegistry.getTargetAppInfo("com.zhiliaoapp.musically")
        assertNull(ttInfo)
    }

    @Test
    fun test2_unknownPackageRejection_rejectsNonAllowlistedPackages() {
        assertFalse(TargetAppRegistry.isTargetApp("com.example.unknown"))
        assertFalse(TargetAppRegistry.isTargetApp("com.whatsapp"))
        assertFalse(TargetAppRegistry.isTargetApp("com.facebook.katana"))
        assertFalse(TargetAppRegistry.isTargetApp(""))
        assertFalse(TargetAppRegistry.isTargetApp(null))

        assertNull(TargetAppRegistry.getTargetAppInfo("com.unknown.app"))

        // Event processor rejection
        val result = eventProcessor.processEventData("com.whatsapp") { null }
        assertFalse("Non-target app event must not be processed", result.isProcessed)
        assertFalse(result.isTargetApp)
        assertEquals("Package not registered in TargetAppRegistry", result.rejectionReason)
        assertNull(lastDetectedPackage)
    }

    @Test
    fun test3_nullAndUnexpectedAccessibilityEvents_handlesSafelyWithoutCrashing() {
        // Null event passed to processEvent
        val nullResult = eventProcessor.processEvent(null) { null }
        assertFalse(nullResult.isProcessed)
        assertEquals("Null accessibility event", nullResult.rejectionReason)

        // Blank package
        val blankResult = eventProcessor.processEventData("") { null }
        assertFalse(blankResult.isProcessed)
        assertEquals("Empty or null package name", blankResult.rejectionReason)

        // Null root node provider
        val validAppNullNodeResult = eventProcessor.processEventData("com.google.android.youtube") { null }
        assertTrue("Valid package with null root node should still be acknowledged without crash", validAppNullNodeResult.isProcessed)
        assertTrue(validAppNullNodeResult.isTargetApp)
        assertEquals("YouTube", lastDetectedAppName)
    }

    @Test
    fun test6_allowlistedPackageFiltering_containsOnlySupportedApplications() {
        val supported = TargetAppRegistry.SUPPORTED_PACKAGES
        assertEquals(2, supported.size)
        assertTrue(supported.contains("com.google.android.youtube"))
        assertTrue(supported.contains("com.instagram.android"))
        assertFalse(supported.contains("com.zhiliaoapp.musically"))

        val allApps = TargetAppRegistry.getAllTargetApps()
        assertEquals(2, allApps.size)
    }

    @Test
    fun test7_accessibilityEventProcessorBehavior_processesAndThrottles() {
        // First event -> processed
        val result1 = eventProcessor.processEventData("com.instagram.android") { null }
        assertTrue(result1.isProcessed)
        assertTrue(result1.isTargetApp)
        assertEquals("com.instagram.android", lastDetectedPackage)
        assertEquals("Instagram", lastDetectedAppName)

        // Immediate duplicate event -> throttled
        val result2 = eventProcessor.processEventData("com.instagram.android") { null }
        assertFalse("Rapid consecutive event must be throttled", result2.isProcessed)
        assertEquals("Throttled event within throttle window", result2.rejectionReason)
    }

    @Test
    fun test8_nodeInspectionPrivacyRestrictions_enforcesZeroDataCollection() {
        // Test 8A: Password field must be ignored
        val passwordNode = FakeAccessibleNode(
            isPassword = true,
            className = "android.widget.EditText",
            viewIdResourceName = "com.google.android.youtube:id/shorts_player"
        )
        val passwordSummary = nodeInspector.inspectAccessibleNode(passwordNode, "com.google.android.youtube")
        assertTrue("Password nodes must never be examined or matched", passwordSummary.matchedViewIds.isEmpty())

        // Test 8B: User text input (EditText) must be ignored
        val editTextInputNode = FakeAccessibleNode(
            isPassword = false,
            className = "android.widget.EditText",
            viewIdResourceName = "com.google.android.youtube:id/shorts_container"
        )
        val inputSummary = nodeInspector.inspectAccessibleNode(editTextInputNode, "com.google.android.youtube")
        assertTrue("User text input nodes must never be examined", inputSummary.matchedViewIds.isEmpty())

        // Test 8C: Non-allowlisted package returns empty summary immediately
        val unknownSummary = nodeInspector.inspectAccessibleNode(
            FakeAccessibleNode(viewIdResourceName = "com.other:id/shorts_player"),
            "com.other.app"
        )
        assertEquals(0, unknownSummary.totalNodesExamined)
        assertTrue(unknownSummary.matchedViewIds.isEmpty())

        // Test 8D: Structural container nodes are safely matched while user content is omitted
        val structuralNode = FakeAccessibleNode(
            isPassword = false,
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.google.android.youtube:id/shorts_player"
        )
        val structuralSummary = nodeInspector.inspectAccessibleNode(structuralNode, "com.google.android.youtube")
        assertEquals(1, structuralSummary.matchedViewIds.size)
        assertTrue(structuralSummary.matchedViewIds.contains("shorts_player"))
        // Phase 5 restriction: content blocking must be false
        assertFalse(structuralSummary.isRestrictedContentDetected)
    }
}

