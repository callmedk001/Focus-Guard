package com.dk.focusguard.services.accessibility

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Common abstraction representing an inspectable accessibility node.
 * Enables thorough unit testing of privacy rules on local JVM without Android framework stubs.
 */
interface AccessibleNode {
    val isPassword: Boolean
    val isSelected: Boolean get() = false
    val className: CharSequence?
    val viewIdResourceName: String?
    val contentDescription: CharSequence?
    val text: CharSequence? get() = null
    val childCount: Int
    fun getChild(index: Int): AccessibleNode?
}

/**
 * Adapter wrapping real Android framework AccessibilityNodeInfo.
 */
class AndroidAccessibleNode(private val node: AccessibilityNodeInfo) : AccessibleNode {
    override val isPassword: Boolean get() = node.isPassword
    override val isSelected: Boolean get() = node.isSelected
    override val className: CharSequence? get() = node.className
    override val viewIdResourceName: String? get() = node.viewIdResourceName
    override val contentDescription: CharSequence? get() = node.contentDescription
    override val text: CharSequence? get() = node.text
    override val childCount: Int get() = node.childCount
    override fun getChild(index: Int): AccessibleNode? {
        val child = runCatching { node.getChild(index) }.getOrNull() ?: return null
        return AndroidAccessibleNode(child)
    }
}

/**
 * Minimal, privacy-safe metadata extracted from an accessibility node hierarchy.
 * Contains only structural layout clues, never user data, keystrokes, or messages.
 */
data class InspectedNodeSummary(
    val packageName: String,
    val matchedViewIds: Set<String> = emptySet(),
    val matchedClues: Set<String> = emptySet(),
    val totalNodesExamined: Int = 0,
    val isRestrictedContentDetected: Boolean = false
)

/**
 * Privacy-first accessibility node inspector.
 *
 * PRIVACY GUARANTEES:
 * 1. STRICT ALLOWLIST: Operates exclusively on verified target applications.
 * 2. NO PASSWORD INSPECTION: Password nodes (isPassword == true) are strictly skipped.
 * 3. NO INPUT INSPECTION: Editable text inputs (EditText) are strictly skipped.
 * 4. NO SCREENSHOTS: Never invokes takeScreenshot or screen capture APIs.
 * 5. NO KEYLOGGING: Never intercepts or records keyboard input events.
 * 6. NO PRIVATE MESSAGE COLLECTION: Never reads chat messages, emails, or user profiles.
 * 7. NO NETWORK TRANSMISSION: Never transmits accessibility data outside the local process.
 * 8. NO RAW TREE STORAGE: Never retains references to raw AccessibilityNodeInfo trees.
 * 9. DEPTH & NODE LIMITS: Bounds traversal to prevent CPU spikes and excessive memory usage.
 */
class AccessibilityNodeInspector {

    companion object {
        const val MAX_TRAVERSAL_DEPTH = 18
        const val MAX_NODES_EXAMINED = 150

        // Structural resource identifiers associated with short-form video sections
        val KNOWN_SHORT_FORM_IDS = setOf(
            "shorts_container",
            "shorts_player",
            "reel_viewer_container",
            "reel_recycler_view",
            "vertical_feed",
            "reel_viewer"
        )
    }

    /**
     * Production entry point: inspects real Android AccessibilityNodeInfo.
     */
    fun inspectNodeHierarchy(
        rootNode: AccessibilityNodeInfo?,
        packageName: String
    ): InspectedNodeSummary {
        val accessibleNode = rootNode?.let { AndroidAccessibleNode(it) }
        return inspectAccessibleNode(accessibleNode, packageName)
    }

    /**
     * Core inspection logic operating on the AccessibleNode abstraction.
     * Enforces all privacy constraints, depth limits, and node bounds.
     */
    fun inspectAccessibleNode(
        rootNode: AccessibleNode?,
        packageName: String
    ): InspectedNodeSummary {
        // Enforce Allowlist Rule
        if (!TargetAppRegistry.isTargetApp(packageName)) {
            return InspectedNodeSummary(packageName = packageName)
        }

        if (rootNode == null) {
            return InspectedNodeSummary(packageName = packageName)
        }

        val foundIds = mutableSetOf<String>()
        val foundClues = mutableSetOf<String>()
        var nodesCount = 0

        fun traverse(node: AccessibleNode?, depth: Int) {
            if (node == null || depth > MAX_TRAVERSAL_DEPTH || nodesCount >= MAX_NODES_EXAMINED) {
                return
            }

            nodesCount++

            // Privacy Rule 1: Never inspect password fields
            if (node.isPassword) {
                return
            }

            // Privacy Rule 2: Never inspect user text inputs (EditText)
            val className = node.className?.toString() ?: ""
            if (className.contains("EditText", ignoreCase = true)) {
                return
            }

            // Inspect structural View ID only (not user input text)
            val viewId = node.viewIdResourceName
            if (!viewId.isNullOrBlank()) {
                val cleanId = viewId.substringAfter(":id/")
                if (KNOWN_SHORT_FORM_IDS.any { cleanId.contains(it, ignoreCase = true) }) {
                    foundIds.add(cleanId)
                }
            }

            // Check specific system content descriptions (e.g. "Shorts", "Reels" tab buttons)
            val contentDesc = node.contentDescription?.toString()
            if (!contentDesc.isNullOrBlank()) {
                if (contentDesc.equals("Shorts", ignoreCase = true) ||
                    contentDesc.equals("Reels", ignoreCase = true)
                ) {
                    foundClues.add(contentDesc)
                }
            }

            // Recurse into children within depth limits
            for (i in 0 until node.childCount) {
                if (nodesCount >= MAX_NODES_EXAMINED) break
                val child = runCatching { node.getChild(i) }.getOrNull()
                traverse(child, depth + 1)
            }
        }

        traverse(rootNode, 0)

        return InspectedNodeSummary(
            packageName = packageName,
            matchedViewIds = foundIds,
            matchedClues = foundClues,
            totalNodesExamined = nodesCount,
            isRestrictedContentDetected = false // NOTE: Phase 5 restricts actual blocking
        )
    }
}
