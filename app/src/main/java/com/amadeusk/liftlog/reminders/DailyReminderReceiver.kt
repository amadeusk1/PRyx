package com.amadeusk.liftlog.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.amadeusk.liftlog.data.loadReminderEnabled

private const val TAG = "LiftLog"

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        try {
            Log.i(TAG, "DailyReminderReceiver onReceive")
            val name = intent?.getStringExtra(EXTRA_REMINDER_KIND)
                ?: run {
                    Log.w(TAG, "DailyReminderReceiver: missing extra $EXTRA_REMINDER_KIND")
                    return
                }
            val kind = ReminderKind.fromName(name)
                ?: run {
                    Log.w(TAG, "DailyReminderReceiver: unknown kind \"$name\"")
                    return
                }
            if (!loadReminderEnabled(context)) {
                Log.w(TAG, "DailyReminderReceiver: skipped — enable notifications in app Settings")
                return
            }
            ReminderNotificationHelper.show(context, kind)
            DailyReminderScheduler.scheduleNext(context.applicationContext, kind)
        } catch (t: Throwable) {
            Log.e(TAG, "DailyReminderReceiver.onReceive failed", t)
        }
    }
}
