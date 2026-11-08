package com.dk.focusguard.services.accessibility

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Result classification representing the content state within YouTube.
 */
enum class YouTubeClassificationResult {
    /** Confidently identified as active YouTube Shorts viewing */
    SHORTS_CONFIRMED,

    /** Confidently identified as regular YouTube (normal video playback, home feed, search, etc.) */
    NORMAL_YOUTUBE,

    /** UI layout is ambiguous or uncertain; protection rules mandate NO BLOCKING when uncertain */
    UNKNOWN,

    /** Accessibility hierarchy is null, inaccessible, or empty */
    DETECTION_UNAVAILABLE
}

/**
 * Detailed telemetry analysis from the YouTube Shorts inspection.
 * Zero private user data, zero video titles, zero node tree retention.
 */
data class YouTubeDetectionAnalysis(
    val result: YouTubeClassificationResult,
    val confidenceScore: Int,
    val matchedSignals: List<String> = emptyList(),
    val nodesExamined: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Privacy-first, multi-signal classifier for YouTube Shorts.
 *
 * CRITICAL ARCHITECTURAL GUARANTEES:
 * 1. MULTI-SIGNAL: Combines container IDs, action buttons, and pivot bar tab selection states.
 * 2. CONFIDENCE-BASED: Never relies on a single brittle hardcoded label.
 * 3. SAFE DISTINCTION: Specifically protects standard widescreen video watching, search, subscriptions,
 *    and channel pages from false positives.
 * 4. PRIVACY BOUNDED: Strictly ignores password nodes and user input fields (EditText).
 * 5. UNCERTAINTY SAFETY: If detection is ambiguous, evaluates to UNKNOWN and never interrupts.
 * 6. NO TREE STORAGE: Only lightweight signal strings are returned; node instances are discarded immediately.
 */
class YouTubeShortsClassifier : ContentClassifier {

    companion object {
        const val YOUTUBE_PACKAGE = TargetAppRegistry.PACKAGE_YOUTUBE
        const val MAX_TRAVERSAL_DEPTH = 28
        const val MAX_NODES_EXAMINED = 800

        // Strong Shorts Container & Player Resource Clues
        val SHORTS_CONTAINER_IDS = setOf(
            "reel_recycler",
            "reel_recycler_view",
            "shorts_container",
            "shorts_player",
            "shorts_player_view",
            "shorts_playback",
            "shorts_video_view",
            "shorts_surface_view",
            "shorts_player_fragment",
            "reel_player_page_tree",
            "reel_watch_fragment",
            "reel_watch_fragment_root",
            "reel_watch_player",
            "reel_player_view",
            "reel_video_player",
            "reel_container",
            "shorts_shelf",
            "reel_swipe_layout",
            "shorts_pager",
            "shorts_root",
            "reel_holder",
            "reel_item",
            "reel_playback_fragment",
            "reel_viewer",
            "reel_time_bar",
            "reel_player_overlay_root",
            "reel_player_underlay",
            "reel_watch_refresher",
            "reel_video_interactions",
            "reel_recycler_item",
            "reel_root",
            "reel_content",
            "reel_player_view_holder"
        )

        // Shorts-Specific Action Buttons Clues
        val SHORTS_ACTION_IDS = setOf(
            "reel_right_side_layout",
            "reel_sound_pivot_button",
            "reel_remix_button",
            "reel_like_button",
            "reel_dislike_button",
            "reel_comment_button",
            "reel_share_button",
            "reel_pivot_button",
            "shorts_pivot_item",
            "sound_pivot_button",
            "audio_pivot_button",
            "remix_button"
        )

        // Normal YouTube Video Player Identifiers (horizontal/standard player)
        val NORMAL_PLAYER_IDS = setOf(
            "watch_player",
            "player_fragment_container",
            "movie_view",
            "player_view",
            "player_control_container",
            "inline_player_container"
        )

        // Normal YouTube Navigation & Search Identifiers
        val NORMAL_BROWSE_IDS = setOf(
            "results_list",
            "feed_filter_bar",
            "search_results",
            "channel_page",
            "comment_thread",
            "shelf_header"
        )
    }

    override fun supports(packageName: String): Boolean {
        return packageName.equals(YOUTUBE_PACKAGE, ignoreCase = true)
    }

    override fun classify(rootNode: Any?, packageName: String): ContentClassification {
        val accessibleNode = when (rootNode) {
            is AccessibleNode -> rootNode
            is AccessibilityNodeInfo -> AndroidAccessibleNode(rootNode)
            else -> null
        }
        val analysis = classifyNodeHierarchy(accessibleNode)
        val category = when (analysis.result) {
            YouTubeClassificationResult.SHORTS_CONFIRMED -> ContentCategory.YOUTUBE_SHORTS
            YouTubeClassificationResult.NORMAL_YOUTUBE -> ContentCategory.NORMAL_CONTENT
            else -> ContentCategory.UNKNOWN
        }

        return ContentClassification(
            targetApp = TargetAppRegistry.getTargetAppInfo(packageName),
            detectedCategory = category,
            isRestrictedContent = analysis.result == YouTubeClassificationResult.SHORTS_CONFIRMED,
            detectionClues = analysis.matchedSignals,
            timestamp = analysis.timestamp
        )
    }

    /**
     * Inspects the accessibility node tree using the AccessibleNode abstraction.
     * Operates cleanly on both JVM test doubles and Android framework nodes.
     */
    fun classifyNodeHierarchy(rootNode: AccessibleNode?): YouTubeDetectionAnalysis {
        if (rootNode == null) {
            return YouTubeDetectionAnalysis(
                result = YouTubeClassificationResult.DETECTION_UNAVAILABLE,
                confidenceScore = 0,
                matchedSignals = emptyList(),
                nodesExamined = 0
            )
        }

        var nodesExamined = 0
        var shortsScore = 0
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
                val isShortsSpecific = cleanId.contains("reel", ignoreCase = true) || cleanId.contains("shorts", ignoreCase = true)

                // 1. Shorts Container Matches (Weight: +5)
                if (SHORTS_CONTAINER_IDS.any { cleanId.contains(it, ignoreCase = true) }) {
                    shortsScore += 5
                    matchedSignals.add("id:$cleanId (+5)")
                } else if (isShortsSpecific) {
                    shortsScore += 4
                    matchedSignals.add("id:$cleanId (+4)")
                }

                // 2. Shorts Action Buttons Matches (Weight: +2)
                if (SHORTS_ACTION_IDS.any { cleanId.contains(it, ignoreCase = true) }) {
                    shortsScore += 2
                    matchedSignals.add("id:$cleanId (+2)")
                }

                // 3. Normal Player Matches (Weight: -5) - Only when not a Shorts/Reel container
                if (!isShortsSpecific && NORMAL_PLAYER_IDS.any { cleanId.contains(it, ignoreCase = true) }) {
                    normalScore += 5
                    matchedSignals.add("normal_id:$cleanId (-5)")
                }

                // 4. Normal Browse Matches (Weight: -2) - Only when not a Shorts/Reel container
                if (!isShortsSpecific && NORMAL_BROWSE_IDS.any { cleanId.contains(it, ignoreCase = true) }) {
                    normalScore += 2
                    matchedSignals.add("browse_id:$cleanId (-2)")
                }
            }

            // Evaluate Content Description & Tab Selection Clues
            val contentDesc = node.contentDescription?.toString() ?: ""
            val nodeText = node.text?.toString() ?: ""

            if (contentDesc.isNotBlank() || nodeText.isNotBlank()) {
                val isSelected = node.isSelected

                // Check for Shorts pivot tab or Shorts reel markers
                if (contentDesc.contains("Shorts", ignoreCase = true) || nodeText.contains("Shorts", ignoreCase = true)) {
                    if (isSelected || contentDesc.contains("selected", ignoreCase = true)) {
                        // Actively selected Shorts pivot tab (Weight: +4)
                        shortsScore += 4
                        matchedSignals.add("desc:Shorts(Selected) (+4)")
                    } else if (contentDesc.contains("Shorts player", ignoreCase = true) || contentDesc.contains("Shorts video", ignoreCase = true)) {
                        shortsScore += 5
                        matchedSignals.add("desc:ShortsPlayer (+5)")
                    } else {
                        // Unselected Shorts tab icon visible on home pivot bar (Neutral clue: +1)
                        shortsScore += 1
                        matchedSignals.add("desc:Shorts(Tab) (+1)")
                    }
                }

                // Shorts specific action button descriptions (Remix, Sound/Audio pivot)
                if (contentDesc.contains("Remix", ignoreCase = true) || nodeText.equals("Remix", ignoreCase = true)) {
                    shortsScore += 3
                    matchedSignals.add("desc:Remix (+3)")
                }

                if (contentDesc.contains("original sound", ignoreCase = true) ||
                    contentDesc.contains("sound details", ignoreCase = true) ||
                    contentDesc.contains("use this sound", ignoreCase = true)) {
                    shortsScore += 3
                    matchedSignals.add("desc:SoundPivot (+3)")
                }

                // Regular navigation tab selected (Weight: -3)
                if (isSelected && (
                        contentDesc.contains("Home", ignoreCase = true) ||
                        contentDesc.contains("Subscriptions", ignoreCase = true) ||
                        contentDesc.contains("Library", ignoreCase = true) ||
                        contentDesc.contains("You", ignoreCase = true)
                    )) {
                    normalScore += 3
                    matchedSignals.add("desc:$contentDesc(Selected) (-3)")
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
            return YouTubeDetectionAnalysis(
                result = YouTubeClassificationResult.DETECTION_UNAVAILABLE,
                confidenceScore = 0,
                matchedSignals = emptyList(),
                nodesExamined = 0
            )
        }

        val netConfidence = shortsScore - normalScore

        val finalResult = when {
            // Strong Shorts Signals (reel_recycler, reel_watch_player, reel_time_bar, selected tab, remix)
            shortsScore >= 4 -> {
                YouTubeClassificationResult.SHORTS_CONFIRMED
            }

            // Normal video playback or normal home navigation dominant
            normalScore >= 3 && shortsScore < 4 -> {
                YouTubeClassificationResult.NORMAL_YOUTUBE
            }

            // Unselected Shorts tab alone on home feed does not constitute Shorts viewing!
            shortsScore in 1..3 && normalScore >= 1 -> {
                YouTubeClassificationResult.NORMAL_YOUTUBE
            }

            // Weak signals or non-matching layout
            shortsScore == 0 && normalScore == 0 -> {
                YouTubeClassificationResult.UNKNOWN
            }

            else -> {
                YouTubeClassificationResult.UNKNOWN
            }
        }

        return YouTubeDetectionAnalysis(
            result = finalResult,
            confidenceScore = netConfidence,
            matchedSignals = matchedSignals,
            nodesExamined = nodesExamined
        )
    }
}
