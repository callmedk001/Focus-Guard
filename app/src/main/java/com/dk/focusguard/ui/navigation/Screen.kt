package com.dk.focusguard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destination definitions for DK FOCUS GUARD.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Protection : Screen("protection", "Protection", Icons.Default.Security)
    data object Focus : Screen("focus", "Focus", Icons.Default.Timer)
    data object Statistics : Screen("statistics", "Statistics", Icons.Default.BarChart)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Phase 4 Authentication Destinations
    data object PinSetup : Screen("pin_setup", "Set PIN", Icons.Default.Security)
    data object PinUnlock : Screen("pin_unlock/{action}", "Unlock", Icons.Default.Security) {
        fun createRoute(action: String = "authorize"): String = "pin_unlock/$action"
    }
    data object ChangePin : Screen("change_pin", "Change PIN", Icons.Default.Security)

    // Phase 5 Accessibility Engine Destinations
    data object AccessibilitySetup : Screen("accessibility_setup", "Accessibility Setup", Icons.Default.Security)

    companion object {
        val bottomNavItems = listOf(
            Dashboard,
            Protection,
            Focus,
            Statistics,
            Settings
        )
    }
}
