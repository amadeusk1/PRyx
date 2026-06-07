package com.amadeusk.liftlog.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.amadeusk.liftlog.MainActivity
import com.amadeusk.liftlog.R
import com.amadeusk.liftlog.data.loadAggressiveRemindersEnabled
import kotlin.random.Random

// Bump id when channel behavior changes (Android ignores updates to existing channels)
private const val CHANNEL_ID = "pr_reminders_v2"

internal enum class ReminderKind(
    val alarmRequestCode: Int,
    val showRequestCode: Int,
    val notificationId: Int,
    val titleRes: Int,
    val textRes: Int,
    val aggressiveTitleArrayRes: Int,
    val aggressiveTextArrayRes: Int
) {
    BODYWEIGHT(
        alarmRequestCode = 71001,
        showRequestCode = 71011,
        notificationId = 71001,
        titleRes = R.string.reminder_bw_title,
        textRes = R.string.reminder_bw_text,
        aggressiveTitleArrayRes = R.array.reminder_bw_aggressive_titles,
        aggressiveTextArrayRes = R.array.reminder_bw_aggressive_texts
    ),
    LIFT(
        alarmRequestCode = 71002,
        showRequestCode = 71012,
        notificationId = 71002,
        titleRes = R.string.reminder_lift_title,
        textRes = R.string.reminder_lift_text,
        aggressiveTitleArrayRes = R.array.reminder_lift_aggressive_titles,
        aggressiveTextArrayRes = R.array.reminder_lift_aggressive_texts
    );

    companion object {
        fun fromName(name: String): ReminderKind? =
            entries.find { it.name == name }
    }
}

private const val TAG = "LiftLog"

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val w = drawable.intrinsicWidth.coerceAtLeast(1)
    val h = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private const val LARGE_ICON_SIZE_PX = 256
private const val SMALL_ICON_BITMAP_PX = 144

@Volatile
private var cachedSmallNotificationIcon: IconCompat? = null
@Volatile
private var cachedLargeNotificationIcon: Bitmap? = null

private fun smallNotificationIcon(context: Context): IconCompat {
    cachedSmallNotificationIcon?.let { return it }
    val bitmap = appIconBitmap(context, SMALL_ICON_BITMAP_PX)
    val icon = IconCompat.createWithBitmap(bitmap)
    cachedSmallNotificationIcon = icon
    return icon
}

private fun appIconBitmap(context: Context, sizePx: Int): Bitmap {
    val drawable = context.packageManager.getApplicationIcon(context.packageName)
    if (drawable is AdaptiveIconDrawable) {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }
    val raw = drawableToBitmap(drawable)
    return if (raw.width == sizePx && raw.height == sizePx) raw
    else Bitmap.createScaledBitmap(raw, sizePx, sizePx, true)
}

private fun largeIconBitmap(context: Context): Bitmap? {
    cachedLargeNotificationIcon?.let { return it }
    return try {
        appIconBitmap(context, LARGE_ICON_SIZE_PX).also { cachedLargeNotificationIcon = it }
    } catch (_: Exception) {
        try {
            context.getDrawable(R.mipmap.ic_launcher)?.let { d ->
                Bitmap.createScaledBitmap(
                    drawableToBitmap(d),
                    LARGE_ICON_SIZE_PX,
                    LARGE_ICON_SIZE_PX,
                    true
                ).also { cachedLargeNotificationIcon = it }
            }
        } catch (_: Exception) {
            null
        }
    }
}

internal object ReminderNotificationHelper {

    fun ensureChannels(context: Context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.deleteNotificationChannel("daily_reminders")
            nm.deleteNotificationChannel("pr_reminders")
            nm.deleteNotificationChannel("pr_reminders_v2")
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            // HIGH: IMPORTANCE_MAX is flaky on some OEMs for general apps
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                importance
            ).apply {
                description = context.getString(R.string.reminder_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 200, 350)
                enableLights(true)
                setSound(soundUri, audioAttrs)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        } catch (t: Throwable) {
            Log.e(TAG, "ensureChannels failed", t)
        }
    }

    fun show(context: Context, kind: ReminderKind) {
        try {
            ensureChannels(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val canPost = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!canPost) {
                    Log.w(TAG, "POST_NOTIFICATIONS denied — grant notification permission in system settings or app prompt")
                    return
                }
            }
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Log.w(
                    TAG,
                    "App notifications disabled in system settings"
                )
                return
            }
            val tap = android.app.PendingIntent.getActivity(
                context,
                kind.notificationId,
                android.content.Intent(context, MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val aggressive = loadAggressiveRemindersEnabled(context)
            val (title, text) = if (aggressive) {
                val titles = context.resources.getStringArray(kind.aggressiveTitleArrayRes)
                val texts = context.resources.getStringArray(kind.aggressiveTextArrayRes)
                val n = minOf(titles.size, texts.size)
                if (n > 0) {
                    val i = Random.nextInt(n)
                    titles[i] to texts[i]
                } else {
                    context.getString(kind.titleRes) to context.getString(kind.textRes)
                }
            } else {
                context.getString(kind.titleRes) to context.getString(kind.textRes)
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallNotificationIcon(context))
                .setContentTitle(title)
                .setContentText(text)
                .setSubText(context.getString(R.string.app_name))
                .setTicker(title)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(text)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(false)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .apply {
                    largeIconBitmap(context)?.let { setLargeIcon(it) }
                }
                .build()
            NotificationManagerCompat.from(context).notify(kind.notificationId, notification)
            Log.i(TAG, "Posted notification id=${kind.notificationId} ($kind)")
        } catch (t: Throwable) {
            Log.e(TAG, "show notification failed for $kind", t)
        }
    }
}
