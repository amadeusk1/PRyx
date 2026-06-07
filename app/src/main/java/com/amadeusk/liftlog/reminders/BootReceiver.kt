package com.amadeusk.liftlog.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "LiftLog"

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        try {
            DailyReminderScheduler.scheduleAllIfEnabled(context.applicationContext)
        } catch (t: Throwable) {
            Log.e(TAG, "BootReceiver.onReceive failed", t)
        }
    }
}
