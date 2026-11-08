package com.dk.focusguard.services.accessibility

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Result classification representing the content state within Instagram.
 */
enum class InstagramClassificationResult {
    /** Confidently identified as active Instagram Reels consumption */
    REELS_CONFIRMED,

    /** Confidently identified as regular Instagram (Feed, DMs, Stories, Profiles, Explore, Comments) */
    NORMAL_INSTAGRAM,

    /** UI layout is ambiguous or uncertain; protection rules mandate NO BLOCKING when uncertain */
    UNKNOWN,

    /** Accessibility hierarchy is null, inaccessible, or empty */
    DETECTION_UNAVAILABLE
}

/**
 * Detailed telemetry analysis from the Instagram Reels inspection.
 * Zero private user data, zero message text, zero captions, zero node tree retention.
 */
data class InstagramDetectionAnalysis(
    val result: InstagramClassificationResult,
    val confidenceScore: Int,
    val matchedSignals: List<String> = emptyList(),
    val nodesExamined: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Privacy-first, multi-signal classifier for Instagram Reels.
 *
 * CRITICAL ARCHITECTURAL GUARANTEES:
 * 1. MULTI-SIGNAL: Combines Clips viewer containers, action buttons, and pivot bar tab selection states.
 * 2. SAFE DISTINCTION:
 *    - Direct Messages (DMs): Strictly protected from false positives.
 *    - Stories (Story Viewer): Strictly distinguished from Reels infinite scroll.
 *    - Profiles, Explore, Comments, and Normal Home Feed: Strictly preserved.
 * 3. PRIVACY BOUNDED: Strictly ignores password nodes and user input fields (EditText).
 * 4. UNCERTAINTY SAFETY: If detection is ambiguous, evaluates to UNKNOWN and never interrupts.
 * 5. NO TREE STORAGE: Only lightweight signal strings are returned; node instances are discarded immediately.
 */
class InstagramReelsClassifier : ContentClassifier {

    companion object {
        const val INSTAGRAM_PACKAGE = TargetAppRegistry.PACKAGE_INSTAGRAM
        const val MAX_TRAVERSAL_DEPTH = 18
        const val MAX_NODES_EXAMINED = 200

        // Strong Reels (Clips) Container & Player Resource Identifiers
        val REELS_CONTAINER_IDS = setOf(
            "clips_viewer_view_pager",
            "clips_video_container",
            "clips_viewer_container",
            "clips_media_interactivity_view",
            "clips_swipe_refresh_layout",
            "clips_viewer_view_pager_content",
            "clips_video",
            "clips_viewer",
            "reels_tray",
            "clips_media_item"
        )

        // Reels-Specific Action Buttons Clues
        val REELS_ACTION_IDS = setOf(
            "clips_audio_mix_button",
            "clips_remix_button",
            "clips_camera_button",
            "clips_action_bar",
            "clips_right_side_actions"
        )

        // Direct Messages (DMs) Clues - NEVER TO BE BLOCKED!
        val DM_INDICATOR_IDS = setOf(
            "direct_thread_layout",
            "direct_inbox",
            "message_list",
            "direct_message_list",
            "row_thread_title",
            "direct_visual_message_composer",
            "action_bar_title_container"
        )

        // Stories Identifiers (Stories are friend status updates, NOT the infinite algorithmic Reels scroll)
        val STORY_INDICATOR_IDS = setOf(
            "segmented_progress_bar",
            "story_progress_bar",
            "reel_viewer_progress_bar",
            "story_viewer_container",
            "reel_viewer_story_container",
            "story_reply_composer",
            "story_caption"
        )

        // Normal Feed, Profile, Search, and Comment Identifiers
        val NORMAL_BROWSE_IDS = setOf(
            "feed_recycler_view",
            "sticky_header_list",
            "row_feed_photo_profile_name",
            "row_feed_photo_media",
            "profile_header_container",
            "user_profile_header",
            "explore_recycler_view",
            "search_bar_layout",
            "comment_thread_view",
            "layout_comment_thread",
            "comments_list"
        )
    }

    override fun supports(packageName: String): Boolean {
        return packageName.equals(INSTAGRAM_PACKAGE, ignoreCase = true)
    }

    override fun classify(rootNode: Any?, packageName: String): ContentClassification {
        val accessibleNode = when (rootNode) {
            is AccessibleNode -> rootNode
            is AccessibilityNodeInfo -> AndroidAccessibleNode(rootNode)
            else -> null
        }
        val analysis = classifyNodeHierarchy(accessibleNode)
        val category = when (analysis.result) {
            InstagramClassificationResult.REELS_CONFIRMED -> ContentCategory.INSTAGRAM_REELS
            InstagramClassificationResult.NORMAL_INSTAGRAM -> ContentCategory.NORMAL_CONTENT
            else -> ContentCategory.UNKNOWN
        }

        return ContentClassification(
            targetApp = TargetAppRegistry.getTargetAppInfo(packageName),
            detectedCategory = category,
            isRestrictedContent = analysis.result == InstagramClassificationResult.REELS_CONFIRMED,
            detectionClues = analysis.matchedSignals,
            timestamp = analysis.timestamp
        )
    }

    /**
     * Inspects the accessibility node tree using the AccessibleNode abstraction.
     * Operates cleanly on both JVM test doubles and Android framework nodes.
     */
    fun classifyNodeHierarchy(rootNode: AccessibleNode?): InstagramDetectionAnalysis {
        if (rootNode == null) {
            return InstagramDetectionAnalysis(
                result = InstagramClassificationResult.DETECTION_UNAVAILABLE,
                confidenceScore = 0,
                matchedSignals = emptyList(),
                nodesExamined = 0
            )
        }

        var nodesExamined = 0
        var reelsScore = 0
        var normalScore = 0
        val matchedSignals = mutableListOf<String>()

        fun traverse(node: AccessibleNode?, depth: Int) {
            if (node == null || depth > MAX_TRAVERSAL_DEPTH || nodesExamined >= MAX_NODES_EXAMINED) {
                return
            }

            nodesExamined++

            // Privacy Rule 1: Never inspect password fields
            if (node.isPassword) {
                return
            }

            // Privacy Rule 2: Never inspect editable user input
            val className = node.className?.toString() ?: ""
            if (className.contains("EditText", ignoreCase = true)) {
                return
            }

            // Evaluate Resource ID Clues
            val viewId = node.viewIdResourceName
            if (!viewId.isNullOrBlank()) {
                val cleanId = viewId.substringAfter(":id/")

                // 1. Direct Messages Clues (Weight: -8, Critical Privacy Protection)
                if (DM_INDICATOR_IDS.any { cleanId.contains(it, ignoreCase = true) }) {
                    normalScore += 8
                    matchedSignals.add("dm_id:$cleanId (-8)")
                }

                // 2. Story Clues (Weight: -6, Stories are NOT Reels)
                if (STORY_INDICATOR_IDS.any { cleanId.contains(it, ignoreCase = true) }) {
                    normalScore += 6
                    matchedSignals.add("story_id:$cleanId (-6)")
                }

                // 3. Reels Container Matches (Weight: +5)
                if (REELS_CONTAINER_IDS.any { cleanId.contains(it, ignoreCase = true) }) {
                    reelsScore += 5
                    matchedSignals.add("reels_id:$cleanId (+5)")
                }

                // 4. Reels Action Buttons Matches (Weight: +2)
                if (REELS_ACTION_IDS.any { cleanId.contains(it, ignoreCase = true) }) {
                    reelsScore += 2
                    matchedSignals.add("reels_action:$cleanId (+2)")
                }

                // 5. Normal Browse Clues (Feed, Profile, Search, Comments) (Weight: -4)
                if (NORMAL_BROWSE_IDS.any { cleanId.contains(it, ignoreCase = true) }) {
                    normalScore += 4
                    matchedSignals.add("normal_id:$cleanId (-4)")
                }
            }

            // Evaluate Content Description & Tab Selection Clues
            val contentDesc = node.contentDescription?.toString()
            if (!contentDesc.isNullOrBlank()) {
                val isSelected = node.isSelected

                if (contentDesc.contains("Reels", ignoreCase = true)) {
                    if (isSelected || contentDesc.contains("selected", ignoreCase = true)) {
                        // Actively selected Reels tab on bottom navigation (Weight: +4)
                        reelsScore += 4
                        matchedSignals.add("desc:Reels(Selected) (+4)")
                    } else {
                        // Unselected Reels tab visible on home bottom bar (Neutral clue: +1)
                        reelsScore += 1
                        matchedSignals.add("desc:Reels(Tab) (+1)")
                    }
                } else if (contentDesc.contains("Direct", ignoreCase = true) ||
                    contentDesc.contains("Messages", ignoreCase = true) ||
                    contentDesc.contains("Chats", ignoreCase = true)
                ) {
                    // DM indicators in content descriptions
                    normalScore += 6
                    matchedSignals.add("desc:DMs (-6)")
                } else if (isSelected && (
                        contentDesc.contains("Home", ignoreCase = true) ||
                        contentDesc.contains("Profile", ignoreCase = true) ||
                        contentDesc.contains("Search", ignoreCase = true) ||
                        contentDesc.contains("Explore", ignoreCase = true)
                    )) {
                    // Regular navigation tab selected (Weight: -4)
                    normalScore += 4
                    matchedSignals.add("desc:$contentDesc(Selected) (-4)")
                }
            }

            // Recurse into children within depth limits
            for (i in 0 until node.childCount) {
                if (nodesExamined >= MAX_NODES_EXAMINED) break
                val child = runCatching { node.getChild(i) }.getOrNull()
                traverse(child, depth + 1)
            }
        }

        traverse(rootNode, 0)

        if (nodesExamined == 0) {
            return InstagramDetectionAnalysis(
                result = InstagramClassificationResult.DETECTION_UNAVAILABLE,
                confidenceScore = 0,
                matchedSignals = emptyList(),
                nodesExamined = 0
            )
        }

        val netConfidence = reelsScore - normalScore

        val finalResult = when {
            // Direct Messages or Stories dominant -> Must never be blocked
            normalScore >= 6 -> {
                InstagramClassificationResult.NORMAL_INSTAGRAM
            }

            // Strong Reels Signals
            reelsScore >= 4 -> {
                InstagramClassificationResult.REELS_CONFIRMED
            }

            // Unselected Reels tab alone on Home feed does not constitute Reels viewing!
            reelsScore in 1..3 && normalScore in 1..3 -> {
                InstagramClassificationResult.NORMAL_INSTAGRAM
            }

            // Weak or no matching signals
            reelsScore == 0 && normalScore == 0 -> {
                InstagramClassificationResult.UNKNOWN
            }

            else -> {
                InstagramClassificationResult.UNKNOWN
            }
        }

        return InstagramDetectionAnalysis(
            result = finalResult,
            confidenceScore = netConfidence,
            matchedSignals = matchedSignals,
            nodesExamined = nodesExamined
        )
    }
}

