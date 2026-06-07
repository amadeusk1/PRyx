package com.amadeusk.liftlog

import android.app.Application
import android.util.Log
import com.amadeusk.liftlog.reminders.DailyReminderScheduler
import com.amadeusk.liftlog.reminders.ReminderNotificationHelper

class LiftLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Recreate notification channels on every launch so id/importance changes apply without reinstalling
            ReminderNotificationHelper.ensureChannels(this)
            DailyReminderScheduler.scheduleAllIfEnabled(this)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to schedule daily reminders on startup", t)
        }
    }

    companion object {
        private const val TAG = "LiftLog"
    }
}
