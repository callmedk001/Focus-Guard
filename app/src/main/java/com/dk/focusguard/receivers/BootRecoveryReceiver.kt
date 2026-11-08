package com.dk.focusguard.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dk.focusguard.FocusGuardApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered after device restart (or app update) to restore
 * focus schedules, alarm timers, and protection daemons safely without crash loops.
 */
class BootRecoveryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON"
        )

        if (intent.action !in validActions) return

        val app = context.applicationContext as? FocusGuardApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.unifiedProtectionManager.handleBootCompleted()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

