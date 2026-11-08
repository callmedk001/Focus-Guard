package com.dk.focusguard.services.accessibility

/**
 * Metadata representation of a supported application.
 */
data class TargetAppInfo(
    val packageName: String,
    val displayName: String,
    val category: String,
    val description: String
)

/**
 * Categorization of content detected within target applications.
 * Prepared for future content classification engines without active blocking in Phase 5.
 */
enum class ContentCategory {
    NORMAL_CONTENT,
    YOUTUBE_SHORTS,
    INSTAGRAM_REELS,
    UNKNOWN
}

/**
 * Result of inspecting accessibility UI nodes for restricted content patterns.
 */
data class ContentClassification(
    val targetApp: TargetAppInfo?,
    val detectedCategory: ContentCategory = ContentCategory.UNKNOWN,
    val isRestrictedContent: Boolean = false,
    val detectionClues: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Modular classifier interface for future content detection engines.
 */
interface ContentClassifier {
    fun supports(packageName: String): Boolean
    fun classify(rootNode: Any?, packageName: String): ContentClassification
}

/**
 * Maintainable registry of supported target applications.
 * Enforces strict allowlist filtering so non-target applications are completely ignored.
 */
object TargetAppRegistry {

    const val PACKAGE_YOUTUBE = "com.google.android.youtube"
    const val PACKAGE_INSTAGRAM = "com.instagram.android"

    private val TARGET_APPS = mapOf(
        PACKAGE_YOUTUBE to TargetAppInfo(
            packageName = PACKAGE_YOUTUBE,
            displayName = "YouTube",
            category = "Video & Entertainment",
            description = "Monitors vertical Shorts tabs while preserving normal videos and search."
        ),
        PACKAGE_INSTAGRAM to TargetAppInfo(
            packageName = PACKAGE_INSTAGRAM,
            displayName = "Instagram",
            category = "Social Media",
            description = "Monitors Reels consumption while preserving Direct Messages and posts."
        )
    )

    val SUPPORTED_PACKAGES: Set<String> = TARGET_APPS.keys

    /**
     * Determines whether the given package name belongs to a registered target application.
     */
    fun isTargetApp(packageName: CharSequence?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return TARGET_APPS.containsKey(packageName.toString())
    }

    /**
     * Returns metadata for the target application, or null if not registered.
     */
    fun getTargetAppInfo(packageName: String?): TargetAppInfo? {
        if (packageName == null) return null
        return TARGET_APPS[packageName]
    }

    /**
     * Returns an immutable list of all registered target applications.
     */
    fun getAllTargetApps(): List<TargetAppInfo> {
        return TARGET_APPS.values.toList()
    }
}

