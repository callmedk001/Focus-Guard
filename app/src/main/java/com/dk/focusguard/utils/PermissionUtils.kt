package com.dk.focusguard.utils

import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat

/**
 * Utility functions for checking and requesting required system permissions.
 */
object PermissionUtils {

    /**
     * Checks whether the app has Usage Stats access (to detect active foreground apps).
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Intent to open the Usage Access settings screen.
     */
    fun getUsageStatsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    /**
     * Checks whether the app has permission to draw overlays (System Alert Window).
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Intent to open the Draw Over Other Apps settings screen.
     */
    fun getOverlayIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    /**
     * Checks whether the app has notification posting permission (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Checks whether the FocusGuardAccessibilityService is enabled by user in Android Settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val targetServiceCanonical = "${context.packageName}/com.dk.focusguard.services.accessibility.FocusGuardAccessibilityService"
        val targetServiceShort = "${context.packageName}/.services.accessibility.FocusGuardAccessibilityService"

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(targetServiceCanonical, ignoreCase = true) ||
                componentName.equals(targetServiceShort, ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Intent to open the Accessibility settings screen.
     */
    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    /**
     * Checks whether local VPN permission has been granted by user.
     * VpnService.prepare returns null if already granted, or an Intent if user consent is needed.
     */
    fun isVpnPermissionGranted(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    /**
     * Intent to request VPN consent from user, or null if already granted.
     */
    fun getVpnPrepareIntent(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Checks whether the app can schedule exact alarms (Android 12+).
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    /**
     * Checks whether the app is exempt from system battery optimizations (Doze mode / Quick Freeze).
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    /**
     * Intent to request exemption from battery optimizations to maintain continuous protection.
     */
    fun getIgnoreBatteryOptimizationsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}

