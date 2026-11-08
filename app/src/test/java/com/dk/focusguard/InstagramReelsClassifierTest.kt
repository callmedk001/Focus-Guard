package com.dk.focusguard

import com.dk.focusguard.fakes.FakeAccessibleNode
import com.dk.focusguard.services.accessibility.InstagramClassificationResult
import com.dk.focusguard.services.accessibility.InstagramReelsClassifier
import com.dk.focusguard.services.accessibility.TargetAppRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying Instagram Reels content classification.
 * Covers requirements 1-7: Confirmed Reels, Normal Feed, DMs, Profiles, Explore,
 * Stories, Comments, Unknown layouts, and Privacy bounds.
 */
class InstagramReelsClassifierTest {

    private lateinit var classifier: InstagramReelsClassifier

    @Before
    fun setUp() {
        classifier = InstagramReelsClassifier()
    }

    @Test
    fun test1_confirmedReelsDetection_withClipsViewPager() {
        // Scenario 1: Confirmed Reels with clips_viewer_view_pager
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/clips_media_interactivity_view"),
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/clips_audio_mix_button")
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(InstagramClassificationResult.REELS_CONFIRMED, analysis.result)
        assertTrue(analysis.confidenceScore >= 5)
        assertTrue(analysis.matchedSignals.any { it.contains("clips_viewer_view_pager") })
    }

    @Test
    fun test1b_confirmedReelsDetection_withClipsVideoContainer() {
        // Scenario 1b: Confirmed Reels with clips_video_container
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/clips_video_container",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/clips_action_bar")
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(InstagramClassificationResult.REELS_CONFIRMED, analysis.result)
        assertTrue(analysis.confidenceScore >= 5)
    }

    @Test
    fun test1c_confirmedReelsDetection_withSelectedReelsTab() {
        // Scenario 1c: Selected Reels tab in bottom navigation bar
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/tab_bar",
            children = listOf(
                FakeAccessibleNode(
                    contentDescription = "Reels",
                    isSelected = true
                ),
                FakeAccessibleNode(
                    viewIdResourceName = "com.instagram.android:id/clips_camera_button"
                )
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(InstagramClassificationResult.REELS_CONFIRMED, analysis.result)
        assertTrue(analysis.confidenceScore >= 4)
    }

    @Test
    fun test2_normalInstagramFeedNotBlocked() {
        // Scenario 2: Normal Instagram photo/video feed must NOT be blocked
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/feed_recycler_view",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/row_feed_photo_media"),
                FakeAccessibleNode(
                    contentDescription = "Home",
                    isSelected = true
                )
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(InstagramClassificationResult.NORMAL_INSTAGRAM, analysis.result)
        assertTrue(analysis.confidenceScore < 0)
    }

    @Test
    fun test3_directMessagesNotBlocked() {
        // Scenario 3: Direct Messages (DMs) must strictly never be blocked
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/direct_thread_layout",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/message_list"),
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/row_thread_title"),
                FakeAccessibleNode(contentDescription = "Direct Messages")
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(InstagramClassificationResult.NORMAL_INSTAGRAM, analysis.result)
        assertTrue(analysis.confidenceScore <= -8)
    }

    @Test
    fun test4_profilesNotBlocked() {
        // Scenario 4: User and profile views must NOT be blocked
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/profile_header_container",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/user_profile_header"),
                FakeAccessibleNode(
                    contentDescription = "Profile",
                    isSelected = true
                )
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(InstagramClassificationResult.NORMAL_INSTAGRAM, analysis.result)
    }

    @Test
    fun test5_exploreAndSearchNotBlocked() {
        // Scenario 5: Explore grid and search must NOT be blocked
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/explore_recycler_view",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/search_bar_layout"),
                FakeAccessibleNode(
                    contentDescription = "Search and explore",
                    isSelected = true
                )
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(InstagramClassificationResult.NORMAL_INSTAGRAM, analysis.result)
    }

    @Test
    fun test6_storiesNotBlocked() {
        // Scenario 6: Stories with segmented progress bar are friend updates, NOT Reels
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/story_viewer_container",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/segmented_progress_bar"),
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/story_reply_composer")
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(InstagramClassificationResult.NORMAL_INSTAGRAM, analysis.result)
        assertTrue(analysis.confidenceScore <= -6)
    }

    @Test
    fun test7_commentsNotBlocked() {
        // Scenario 7: Post and thread comments must NOT be blocked
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/comment_thread_view",
            children = listOf(
                FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/comments_list")
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(InstagramClassificationResult.NORMAL_INSTAGRAM, analysis.result)
    }

    @Test
    fun test8_unknownLayoutNotBlocked() {
        // Scenario 8: Ambiguous layout must evaluate to UNKNOWN and never block
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/generic_settings_container",
            children = listOf(
                FakeAccessibleNode(className = "android.widget.FrameLayout")
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertEquals(InstagramClassificationResult.UNKNOWN, analysis.result)
    }

    @Test
    fun test9_missingAccessibilityNodes() {
        // Scenario 9: Null root yields DETECTION_UNAVAILABLE
        val analysis = classifier.classifyNodeHierarchy(null)

        assertEquals(InstagramClassificationResult.DETECTION_UNAVAILABLE, analysis.result)
        assertEquals(0, analysis.nodesExamined)
    }

    @Test
    fun test10_privacySkipPasswordAndEditText() {
        // Scenario 10: Password and editable text fields are strictly ignored
        val root = FakeAccessibleNode(
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            children = listOf(
                FakeAccessibleNode(
                    isPassword = true,
                    viewIdResourceName = "com.instagram.android:id/user_password_field"
                ),
                FakeAccessibleNode(
                    className = "android.widget.EditText",
                    viewIdResourceName = "com.instagram.android:id/chat_input_text"
                )
            )
        )

        val analysis = classifier.classifyNodeHierarchy(root)

        assertFalse(analysis.matchedSignals.any { it.contains("user_password_field") })
        assertFalse(analysis.matchedSignals.any { it.contains("chat_input_text") })
    }

    @Test
    fun test11_contentClassifierInterfaceConformity() {
        assertTrue(classifier.supports(TargetAppRegistry.PACKAGE_INSTAGRAM))
        assertFalse(classifier.supports("com.google.android.youtube"))

        val root = FakeAccessibleNode(viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager")
        val classification = classifier.classify(root, TargetAppRegistry.PACKAGE_INSTAGRAM)

        assertTrue(classification.isRestrictedContent)
        assertEquals(TargetAppRegistry.PACKAGE_INSTAGRAM, classification.targetApp?.packageName)
    }
}

