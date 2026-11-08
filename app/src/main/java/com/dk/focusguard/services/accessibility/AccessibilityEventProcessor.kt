package com.dk.focusguard.services.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Result of processing an incoming accessibility event.
 */
data class EventProcessingResult(
    val isProcessed: Boolean,
    val packageName: String?,
    val isTargetApp: Boolean,
    val matchedViewIds: Set<String> = emptySet(),
    val rejectionReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * High-performance, low-latency processor for Android Accessibility events.
 * Throttles rapid window change notifications, enforces strict allowlist filtering,
 * and delegates privacy-safe node inspection.
 */
class AccessibilityEventProcessor(
    private val nodeInspector: AccessibilityNodeInspector = AccessibilityNodeInspector(),
    private val throttleIntervalMillis: Long = 50L,
    private val onTargetAppDetected: (packageName: String, appName: String, timestamp: Long) -> Unit = { _, _, _ -> },
    private val onTargetNodeInspected: ((AccessibleNode?, String) -> Unit)? = null
) {

    private var lastProcessedTimestamp: Long = 0L
    private var lastProcessedPackage: String? = null

    /**
     * Processes an incoming Android framework accessibility event.
     * Safely handles null events, unexpected event types, and non-target packages.
     */
    fun processEvent(
        event: AccessibilityEvent?,
        rootNodeProvider: () -> AccessibilityNodeInfo?
    ): EventProcessingResult {
        if (event == null) {
            return EventProcessingResult(
                isProcessed = false,
                packageName = null,
                isTargetApp = false,
                rejectionReason = "Null accessibility event"
            )
        }

        return processEventData(event.packageName) {
            val rootNode = runCatching { rootNodeProvider() }.getOrNull()
            rootNode?.let { AndroidAccessibleNode(it) }
        }
    }

    /**
     * Core processing method accepting raw package name and AccessibleNode provider.
     * Enables clean unit testing without depending on Android AccessibilityEvent instantiation.
     */
    fun processEventData(
        rawPackageName: CharSequence?,
        rootNodeProvider: () -> AccessibleNode?
    ): EventProcessingResult {
        val rawPackage = rawPackageName?.toString()
        if (rawPackage.isNullOrBlank()) {
            return EventProcessingResult(
                isProcessed = false,
                packageName = null,
                isTargetApp = false,
                rejectionReason = "Empty or null package name"
            )
        }

        // Allowlist check: reject non-target applications immediately without inspecting UI
        if (!TargetAppRegistry.isTargetApp(rawPackage)) {
            return EventProcessingResult(
                isProcessed = false,
                packageName = rawPackage,
                isTargetApp = false,
                rejectionReason = "Package not registered in TargetAppRegistry"
            )
        }

        val targetAppInfo = TargetAppRegistry.getTargetAppInfo(rawPackage)
        val now = System.currentTimeMillis()

        // Throttling: avoid duplicate processing within throttleIntervalMillis for content changes
        if (rawPackage == lastProcessedPackage && (now - lastProcessedTimestamp) < throttleIntervalMillis) {
            return EventProcessingResult(
                isProcessed = false,
                packageName = rawPackage,
                isTargetApp = true,
                rejectionReason = "Throttled event within throttle window"
            )
        }

        lastProcessedTimestamp = now
        lastProcessedPackage = rawPackage

        // Notify that a target application is in the foreground
        if (targetAppInfo != null) {
            onTargetAppDetected(rawPackage, targetAppInfo.displayName, now)
        }

        // Perform privacy-bounded structural node inspection
        val rootNode = runCatching { rootNodeProvider() }.getOrNull()
        val inspectionSummary = nodeInspector.inspectAccessibleNode(rootNode, rawPackage)

        onTargetNodeInspected?.invoke(rootNode, rawPackage)

        return EventProcessingResult(
            isProcessed = true,
            packageName = rawPackage,
            isTargetApp = true,
            matchedViewIds = inspectionSummary.matchedViewIds,
            timestamp = now
        )
    }

    fun resetState() {
        lastProcessedTimestamp = 0L
        lastProcessedPackage = null
    }
}

