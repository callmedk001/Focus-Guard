package com.dk.focusguard.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dk.focusguard.FocusGuardApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager at scheduled focus boundaries (start/end).
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val app = context.applicationContext as? FocusGuardApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.unifiedProtectionManager.onScheduleAlarmTriggered()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

