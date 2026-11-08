package com.dk.focusguard.services.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dk.focusguard.MainActivity
import com.dk.focusguard.R
import com.dk.focusguard.domain.model.OverallProtectionStatus
import com.dk.focusguard.domain.model.ProtectionModuleState
import com.dk.focusguard.domain.model.UnifiedProtectionState
import com.dk.focusguard.utils.PermissionUtils

/**
 * Manages the persistent, low-priority protection status notification.
 * Reflects authentic real-time defense posture without clutter or intrusive chimes.
 */
class ProtectionNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "dk_focus_guard_protection_status"
        const val CHANNEL_NAME = "Protection Status"
        const val NOTIFICATION_ID = 1001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live DK Focus Guard defense status indicator"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Updates or posts the protection notification based on current unified state.
     */
    fun updateNotification(state: UnifiedProtectionState) {
        if (!PermissionUtils.hasNotificationPermission(context)) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.createChooser(this, "").flags or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, contentText) = when (state.overallStatus) {
            OverallProtectionStatus.OVERRIDE_ACTIVE -> {
                val mins = (state.overrideRemainingSeconds / 60).coerceAtLeast(1)
                "DK FOCUS GUARD — OVERRIDE ACTIVE" to "All defenses paused. Auto-resumes in ${mins}m"
            }
            OverallProtectionStatus.SCHEDULED -> {
                val scheduleName = state.activeScheduleName ?: "Focus Schedule"
                "DK FOCUS GUARD — SCHEDULE ACTIVE" to "$scheduleName in progress | Defenses armed"
            }
            OverallProtectionStatus.ACTIVE -> {
                val activeModules = mutableListOf<String>()
                if (state.shortsState == ProtectionModuleState.ACTIVE) activeModules.add("Shorts")
                if (state.reelsState == ProtectionModuleState.ACTIVE) activeModules.add("Reels")
                if (state.adultWebsitesState == ProtectionModuleState.ACTIVE) activeModules.add("Adult Filter")
                val modulesDesc = if (activeModules.isEmpty()) "Defenses Armed" else activeModules.joinToString(", ")
                "DK FOCUS GUARD — DEFENSE ARMED" to "Active Shields: $modulesDesc"
            }
            OverallProtectionStatus.OFFLINE -> {
                "DK FOCUS GUARD — SERVICE OFFLINE" to "Background guard daemon stopped. Tap to review."
            }
            OverallProtectionStatus.PERMISSION_REQUIRED -> {
                "DK FOCUS GUARD — ATTENTION NEEDED" to "OS permission required to enforce protection."
            }
            OverallProtectionStatus.INACTIVE -> {
                "DK FOCUS GUARD — PROTECTION INACTIVE" to "Digital discipline shields currently disarmed."
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(state.isGuardingActive)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)

        runCatching {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    /**
     * Cancels the active protection status notification.
     */
    fun cancelNotification() {
        runCatching {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }
}

