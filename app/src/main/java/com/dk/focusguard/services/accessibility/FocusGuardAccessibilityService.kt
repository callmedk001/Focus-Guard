package com.dk.focusguard.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import com.dk.focusguard.FocusGuardApplication
import com.dk.focusguard.domain.model.ProtectionModuleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android Accessibility Service for DK FOCUS GUARD.
 *
 * Responsibilities:
 * 1. Discovers when registered target apps (YouTube, Instagram) are active.
 * 2. Examines minimal accessibility node structure for content categorization.
 * 3. Never captures keystrokes, passwords, screenshots, or private messages.
 * 4. Intercepts YouTube Shorts viewing via safe multi-tier navigation back to Home feed.
 * 5. Intercepts Instagram Reels viewing via safe multi-tier navigation back to Home feed.
 * 6. Communicates real-time connection telemetry to AccessibilityStatusRepository.
 */
class FocusGuardAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var eventProcessor: AccessibilityEventProcessor
    private lateinit var youtubeInterceptionEngine: YouTubeInterceptionEngine
    private lateinit var instagramReelsInterceptionEngine: InstagramReelsInterceptionEngine

    @Volatile
    private var isShortsProtectionEnabled: Boolean = true

    @Volatile
    private var isReelsProtectionEnabled: Boolean = true

    @Volatile
    private var isEmergencyOverrideActive: Boolean = false

    override fun onCreate() {
        super.onCreate()

        val app = application as? FocusGuardApplication

        // Directly observe Unified Protection State for real-time schedule & toggle synchronization
        app?.protectionStateRepository?.let { stateRepo ->
            serviceScope.launch {
                stateRepo.protectionState.collect { state ->
                    isShortsProtectionEnabled = (state.shortsState == ProtectionModuleState.ACTIVE ||
                            state.shortsState == ProtectionModuleState.SCHEDULED)
                    isReelsProtectionEnabled = (state.reelsState == ProtectionModuleState.ACTIVE ||
                            state.reelsState == ProtectionModuleState.SCHEDULED)
                    isEmergencyOverrideActive = state.isEmergencyOverrideActive
                    android.util.Log.d("FocusGuardA11y", "Unified state updated: shorts=$isShortsProtectionEnabled, reels=$isReelsProtectionEnabled, override=$isEmergencyOverrideActive")
                }
            }
        } ?: run {
            app?.protectionSettingsRepository?.let { repo ->
                serviceScope.launch {
                    repo.getProtectionSettings().collect { settings ->
                        isShortsProtectionEnabled = settings.blockYouTubeShorts && settings.isMasterProtectionEnabled
                        isReelsProtectionEnabled = settings.blockInstagramReels && settings.isMasterProtectionEnabled
                        android.util.Log.d("FocusGuardA11y", "Settings updated: shorts=$isShortsProtectionEnabled, reels=$isReelsProtectionEnabled")
                    }
                }
            }
            app?.securityRepository?.let { repo ->
                serviceScope.launch {
                    repo.isEmergencyOverrideActive.collect { overrideActive ->
                        isEmergencyOverrideActive = overrideActive
                        android.util.Log.d("FocusGuardA11y", "Emergency override: $isEmergencyOverrideActive")
                    }
                }
            }
        }

        // YouTube Shorts Interception Engine: Closes Shorts only, keeps YouTube open!
        youtubeInterceptionEngine = YouTubeInterceptionEngine(
            classifier = YouTubeShortsClassifier(),
            isShortsProtectionEnabled = { isShortsProtectionEnabled },
            isEmergencyOverrideActive = { isEmergencyOverrideActive },
            performBackAction = { 
                exitShortsInsideYouTube()
            },
            triggerOverlay = {
                showToast("YouTube Shorts blocked — Focus Guard")
            },
            logEvent = { event ->
                app?.blockEventRepository?.recordRealBlockEvent(event) ?: Result.success(0L)
            },
            coroutineScope = serviceScope
        )

        // Instagram Reels Interception Engine: Closes Reels only, keeps Instagram open!
        instagramReelsInterceptionEngine = InstagramReelsInterceptionEngine(
            classifier = InstagramReelsClassifier(),
            isReelsProtectionEnabled = { isReelsProtectionEnabled },
            isEmergencyOverrideActive = { isEmergencyOverrideActive },
            performBackAction = { 
                exitReelsInsideInstagram()
            },
            triggerOverlay = {
                showToast("Instagram Reels blocked — Focus Guard")
            },
            logEvent = { event ->
                app?.blockEventRepository?.recordRealBlockEvent(event) ?: Result.success(0L)
            },
            coroutineScope = serviceScope
        )

        eventProcessor = AccessibilityEventProcessor(
            nodeInspector = AccessibilityNodeInspector(),
            onTargetAppDetected = { packageName, appName, timestamp ->
                getRepository()?.updateForegroundApp(packageName, appName, timestamp)
            },
            onTargetNodeInspected = { node, packageName ->
                when (packageName) {
                    TargetAppRegistry.PACKAGE_YOUTUBE -> {
                        val decision = youtubeInterceptionEngine.evaluateAndEnforce(node, packageName)
                        android.util.Log.d("FocusGuardA11y", "YouTube outcome: ${decision.outcome} (${decision.message})")
                        instagramReelsInterceptionEngine.resetInterruptionSession()
                    }
                    TargetAppRegistry.PACKAGE_INSTAGRAM -> {
                        val decision = instagramReelsInterceptionEngine.evaluateAndEnforce(node, packageName)
                        android.util.Log.d("FocusGuardA11y", "Instagram outcome: ${decision.outcome} (${decision.message})")
                        youtubeInterceptionEngine.resetInterruptionSession()
                    }
                    else -> {
                        youtubeInterceptionEngine.resetInterruptionSession()
                        instagramReelsInterceptionEngine.resetInterruptionSession()
                    }
                }
            }
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configure service parameters dynamically to ensure optimal performance and complete view visibility
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_VIEW_SELECTED
            packageNames = TargetAppRegistry.SUPPORTED_PACKAGES.toTypedArray()
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info

        getRepository()?.setServiceConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        runCatching {
            val pkg = event.packageName?.toString() ?: ""
            if (TargetAppRegistry.isTargetApp(pkg)) {
                android.util.Log.v("FocusGuardA11y", "Target event: type=${event.eventType} pkg=$pkg")
            }
            eventProcessor.processEvent(event) {
                // Returns active root window node using multi-tier resolution
                getActiveRootNode(event)
            }
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted by system
    }

    override fun onUnbind(intent: Intent?): Boolean {
        getRepository()?.setServiceConnected(false)
        eventProcessor.resetState()
        youtubeInterceptionEngine.resetInterruptionSession()
        instagramReelsInterceptionEngine.resetInterruptionSession()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        getRepository()?.setServiceConnected(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Resiliently obtains the root window node of the active target app.
     * Prevents inspecting isolated leaf nodes or returning null during transient window animations.
     */
    private fun getActiveRootNode(event: AccessibilityEvent?): AccessibilityNodeInfo? {
        // Priority 1: Direct root node of active window
        val activeRoot = runCatching { rootInActiveWindow }.getOrNull()
        if (activeRoot != null) return activeRoot

        // Priority 2: Search interactive application windows
        val winList = runCatching { windows }.getOrNull()
        if (winList != null) {
            for (w in winList) {
                if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION && w.isActive) {
                    val root = runCatching { w.root }.getOrNull()
                    if (root != null) return root
                }
            }
            for (w in winList) {
                if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    val root = runCatching { w.root }.getOrNull()
                    if (root != null) return root
                }
            }
        }

        // Priority 3: Ascend from event.source to root ancestor
        var src = runCatching { event?.source }.getOrNull()
        while (src != null) {
            val parent = runCatching { src.parent }.getOrNull()
            if (parent != null) {
                src = parent
            } else {
                return src
            }
        }
        return null
    }

    private fun exitShortsInsideYouTube(): Boolean {
        android.util.Log.i("FocusGuardA11y", "Exiting Shorts inside YouTube (keeping YouTube open)")

        val root = getActiveRootNode(null)

        // 1. First attempt: Click the "Home" or "Subscriptions" tab to cleanly switch away from Shorts
        if (root != null) {
            val clickedHome = findAndClickTab(root, listOf("Home", "Subscriptions"))
            if (clickedHome) {
                android.util.Log.i("FocusGuardA11y", "Switched to Home tab inside YouTube successfully")
                return true
            }
        }

        // 2. Second attempt: Look for and click the Navigate Up / Back arrow in the Shorts header
        if (root != null) {
            val clickedNavUp = findAndClickNavigationUp(root)
            if (clickedNavUp) {
                android.util.Log.i("FocusGuardA11y", "Clicked Navigate Up inside Shorts header successfully")
                return true
            }
        }

        // 3. Third attempt: Dispatch BACK action within YouTube to exit Shorts without closing YouTube
        val backExecuted = performGlobalAction(GLOBAL_ACTION_BACK)
        android.util.Log.i("FocusGuardA11y", "Dispatched BACK action inside YouTube: $backExecuted")

        // 4. Fourth attempt (Infallible Fallback): Launch YouTube's Main Home Screen Intent.
        // This resets YouTube directly to the Home tab and stops Shorts playback without exiting to Launcher.
        runCatching {
            val homeIntent = packageManager.getLaunchIntentForPackage(TargetAppRegistry.PACKAGE_YOUTUBE)?.apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            if (homeIntent != null) {
                startActivity(homeIntent)
                android.util.Log.i("FocusGuardA11y", "Launched YouTube Home Intent successfully")
                return true
            }
        }

        return backExecuted
    }

    private fun exitReelsInsideInstagram(): Boolean {
        android.util.Log.i("FocusGuardA11y", "Exiting Reels inside Instagram (keeping Instagram open)")

        val root = getActiveRootNode(null)

        // 1. First attempt: Click the "Home" or "Feed" tab to cleanly switch away from Reels
        if (root != null) {
            val clickedHome = findAndClickTab(root, listOf("Home", "Feed"))
            if (clickedHome) {
                android.util.Log.i("FocusGuardA11y", "Switched to Home tab inside Instagram successfully")
                return true
            }
        }

        // 2. Second attempt: Look for and click Navigate Up / Close in Reels header
        if (root != null) {
            val clickedNavUp = findAndClickNavigationUp(root)
            if (clickedNavUp) {
                android.util.Log.i("FocusGuardA11y", "Clicked Navigate Up inside Reels header successfully")
                return true
            }
        }

        // 3. Third attempt: Dispatch BACK action within Instagram to exit Reels
        val backExecuted = performGlobalAction(GLOBAL_ACTION_BACK)
        android.util.Log.i("FocusGuardA11y", "Dispatched BACK action inside Instagram: $backExecuted")

        // 4. Fourth attempt: Launch Instagram Main Feed Intent
        runCatching {
            val homeIntent = packageManager.getLaunchIntentForPackage(TargetAppRegistry.PACKAGE_INSTAGRAM)?.apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            if (homeIntent != null) {
                startActivity(homeIntent)
                return true
            }
        }

        return backExecuted
    }

    private fun findAndClickNavigationUp(node: AccessibilityNodeInfo): Boolean {
        val desc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val isNavUp = desc.equals("Navigate up", ignoreCase = true) ||
                desc.equals("Back", ignoreCase = true) ||
                desc.equals("Close", ignoreCase = true) ||
                viewId.endsWith(":id/action_bar_root") ||
                viewId.contains("up_button", ignoreCase = true) ||
                viewId.contains("navigation_button", ignoreCase = true)

        if (isNavUp) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null && clickableNode.isClickable) {
                val clicked = clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
            if (findAndClickNavigationUp(child)) return true
        }
        return false
    }

    private fun findAndClickTab(node: AccessibilityNodeInfo, targetNames: List<String>): Boolean {
        val desc = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""

        val isTarget = targetNames.any { target ->
            desc.equals(target, ignoreCase = true) ||
            desc.startsWith(target, ignoreCase = true) ||
            text.equals(target, ignoreCase = true)
        }

        if (isTarget) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null && clickableNode.isClickable) {
                val clicked = clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    return true
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
            val found = findAndClickTab(child, targetNames)
            if (found) return true
        }

        return false
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getRepository() = (application as? FocusGuardApplication)?.accessibilityStatusRepository
}
