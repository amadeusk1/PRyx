package com.amadeusk.liftlog.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.amadeusk.liftlog.MainActivity
import com.amadeusk.liftlog.data.loadReminderEnabled
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

internal const val EXTRA_REMINDER_KIND = "extra_reminder_kind"

private const val TAG = "LiftLog"

/**
 * Next trigger: random local time later today, or random time tomorrow if no slots left today.
 * One alarm per kind per cycle keeps Android 14+ exact-alarm usage low.
 */
private fun nextRandomTriggerInDayMillis(): Long {
    val zone = ZoneId.systemDefault()
    val now = ZonedDateTime.now(zone)
    val today = now.toLocalDate()
    var candidate = ZonedDateTime.of(
        today,
        LocalTime.of(Random.nextInt(24), Random.nextInt(60)),
        zone
    )
    if (!candidate.isAfter(now)) {
        val nextDay = today.plusDays(1)
        candidate = ZonedDateTime.of(
            nextDay,
            LocalTime.of(Random.nextInt(24), Random.nextInt(60)),
            zone
        )
    }
    return candidate.toInstant().toEpochMilli()
}

object DailyReminderScheduler {

    fun scheduleAllIfEnabled(context: Context) {
        try {
            if (!loadReminderEnabled(context)) {
                cancelAll(context)
                return
            }
            ReminderNotificationHelper.ensureChannels(context)
            for (kind in ReminderKind.entries) {
                scheduleNext(context, kind)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "scheduleAllIfEnabled failed", t)
        }
    }

    internal fun scheduleNext(context: Context, kind: ReminderKind) {
        try {
            if (!loadReminderEnabled(context)) return
            val trigger = nextRandomTriggerInDayMillis()
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val op = pendingBroadcast(context, kind)
            val show = PendingIntent.getActivity(
                context,
                kind.showRequestCode,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarm(am, trigger, op, show, kind)
        } catch (t: Throwable) {
            Log.e(TAG, "scheduleNext failed for $kind", t)
        }
    }

    fun cancelAll(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            for (kind in ReminderKind.entries) {
                am.cancel(pendingBroadcast(context, kind))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "cancelAll failed", t)
        }
    }

    private fun pendingBroadcast(context: Context, kind: ReminderKind): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_KIND, kind.name)
        }
        return PendingIntent.getBroadcast(
            context,
            kind.alarmRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Use [AlarmManager.setAlarmClock] for daily reminders: reliable in Doze, does not count toward
     * exact-alarm quotas the same way as high-frequency [AlarmManager.setExactAndAllowWhileIdle] use.
     * Falls back to exact / inexact APIs only if setAlarmClock fails.
     */
    private fun scheduleAlarm(
        am: AlarmManager,
        trigger: Long,
        op: PendingIntent,
        show: PendingIntent,
        kind: ReminderKind
    ) {
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), op)
            Log.i(TAG, "setAlarmClock $kind at $trigger (${Instant.ofEpochMilli(trigger)})")
        } catch (e: Exception) {
            Log.w(TAG, "setAlarmClock failed for $kind, trying exact alarm", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, op)
                } else {
                    @Suppress("DEPRECATION")
                    am.setExact(AlarmManager.RTC_WAKEUP, trigger, op)
                }
                Log.i(TAG, "setExact* $kind at $trigger")
            } catch (e2: SecurityException) {
                Log.w(TAG, "exact alarm denied for $kind, using set()", e2)
                @Suppress("DEPRECATION")
                am.set(AlarmManager.RTC_WAKEUP, trigger, op)
            }
        }
    }
}
