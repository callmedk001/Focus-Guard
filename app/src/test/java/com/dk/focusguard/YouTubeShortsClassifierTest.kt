package com.dk.focusguard

import com.dk.focusguard.fakes.FakeAccessibleNode
import com.dk.focusguard.services.accessibility.TargetAppRegistry
import com.dk.focusguard.services.accessibility.YouTubeClassificationResult
import com.dk.focusguard.services.accessibility.YouTubeShortsClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying YouTube Shorts content classification.
 * Covers requirements 1-4, 13 and multi-signal privacy boundaries.
 */
class YouTubeShortsClassifierTest {

    private lateinit var classifier: YouTubeShortsClassifier

    @Before
    fun setUp() {
        classifier = YouTubeShortsClassifier()
    }

    @Test
    fun testConfirmedShortsWithReelRecycler() {
        // Scenario 1: Confirmed Shorts with reel_recycler view ID
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.google.android.youtube:id/reel_recycler",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/reel_player_view"),
                FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/reel_sound_pivot_button")
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(YouTubeClassificationResult.SHORTS_CONFIRMED, analysis.result)
        assertTrue(analysis.confidenceScore >= 5)
        assertTrue(analysis.matchedSignals.any { it.contains("reel_recycler") })
    }

    @Test
    fun testConfirmedShortsWithShortsPlayer() {
        // Scenario 1b: Confirmed Shorts with shorts_player view ID
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.google.android.youtube:id/shorts_player",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/reel_right_side_layout")
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(YouTubeClassificationResult.SHORTS_CONFIRMED, analysis.result)
        assertTrue(analysis.confidenceScore >= 5)
    }

    @Test
    fun testConfirmedShortsWithSelectedPivotTabAndActionButtons() {
        // Scenario 1c: Selected Shorts tab in pivot bar with Shorts action button
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.google.android.youtube:id/pivot_bar",
            children = listOf(
                FakeAccessibleNode(
                    contentDescription = "Shorts",
                    isSelected = true
                ),
                FakeAccessibleNode(
                    viewIdResourceName = "com.google.android.youtube:id/reel_remix_button"
                )
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(YouTubeClassificationResult.SHORTS_CONFIRMED, analysis.result)
        assertTrue(analysis.confidenceScore >= 4)
    }

    @Test
    fun testNormalYouTubeVideoWithWatchPlayer() {
        // Scenario 2: Normal YouTube widescreen video playback
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.google.android.youtube:id/watch_player",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/player_fragment_container"),
                FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/watch_while_layout"),
                FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/comment_thread")
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(YouTubeClassificationResult.NORMAL_YOUTUBE, analysis.result)
        assertTrue(analysis.confidenceScore < 0)
    }

    @Test
    fun testNormalYouTubeBrowseFeed() {
        // Scenario 13: Normal YouTube browsing remains completely unaffected
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.google.android.youtube:id/results_list",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/feed_filter_bar"),
                FakeAccessibleNode(
                    contentDescription = "Home",
                    isSelected = true
                )
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(YouTubeClassificationResult.NORMAL_YOUTUBE, analysis.result)
    }

    @Test
    fun testUnselectedShortsTabOnHomeFeedIsNormal() {
        // When user is on Home tab, an unselected "Shorts" tab icon in the bottom bar must NOT trigger a block!
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.google.android.youtube:id/pivot_bar",
            children = listOf(
                FakeAccessibleNode(
                    contentDescription = "Home",
                    isSelected = true
                ),
                FakeAccessibleNode(
                    contentDescription = "Shorts",
                    isSelected = false
                ),
                FakeAccessibleNode(
                    contentDescription = "Subscriptions",
                    isSelected = false
                )
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(YouTubeClassificationResult.NORMAL_YOUTUBE, analysis.result)
    }

    @Test
    fun testUnknownUIClassification() {
        // Scenario 3: Unknown UI structure with no matching clues
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.google.android.youtube:id/generic_container",
            children = listOf(
                FakeAccessibleNode(className = "android.widget.FrameLayout"),
                FakeAccessibleNode(className = "android.view.View")
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(YouTubeClassificationResult.UNKNOWN, analysis.result)
    }

    @Test
    fun testMissingAccessibilityNodes() {
        // Scenario 4: Null root node yields DETECTION_UNAVAILABLE
        val analysis = classifier.classifyNodeHierarchy(null)

        assertEquals(YouTubeClassificationResult.DETECTION_UNAVAILABLE, analysis.result)
        assertEquals(0, analysis.nodesExamined)
        assertEquals(0, analysis.confidenceScore)
    }

    @Test
    fun testPrivacySkipPasswordAndEditText() {
        // Verifies privacy: password fields and editable inputs are ignored
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.google.android.youtube:id/reel_recycler",
            children = listOf(
                FakeAccessibleNode(
                    isPassword = true,
                    viewIdResourceName = "com.google.android.youtube:id/secret_input"
                ),
                FakeAccessibleNode(
                    className = "android.widget.EditText",
                    viewIdResourceName = "com.google.android.youtube:id/search_query"
                )
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertFalse(analysis.matchedSignals.any { it.contains("secret_input") })
        assertFalse(analysis.matchedSignals.any { it.contains("search_query") })
    }

    @Test
    fun testContentClassifierInterfaceConformity() {
        // Verifies implementation of ContentClassifier interface
        assertTrue(classifier.supports(TargetAppRegistry.PACKAGE_YOUTUBE))
        assertFalse(classifier.supports("com.instagram.android"))

        val root = FakeAccessibleNode(viewIdResourceName = "com.google.android.youtube:id/reel_recycler")
        val classification = classifier.classify(root, TargetAppRegistry.PACKAGE_YOUTUBE)

        assertTrue(classification.isRestrictedContent)
        assertEquals(TargetAppRegistry.PACKAGE_YOUTUBE, classification.targetApp?.packageName)
    }

    @Test
    fun testConfirmedShortsWithRemixAndSoundPivotSignals() {
        // Multi-signal detection with Remix button description and sound pivot
        val root = FakeAccessibleNode(
            className = "android.widget.FrameLayout",
            children = listOf(
                FakeAccessibleNode(
                    contentDescription = "Remix this video"
                ),
                FakeAccessibleNode(
                    contentDescription = "sound details"
                )
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)
        assertEquals(YouTubeClassificationResult.SHORTS_CONFIRMED, analysis.result)
        assertTrue(analysis.matchedSignals.any { it.contains("Remix") })
        assertTrue(analysis.matchedSignals.any { it.contains("SoundPivot") })
    }

    @Test
    fun testConfirmedShortsWithShortsPlayerView() {
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.google.android.youtube:id/shorts_player_view"
        )

        val analysis = classifier.classifyNodeHierarchy(root)
        assertEquals(YouTubeClassificationResult.SHORTS_CONFIRMED, analysis.result)
    }
}

