package com.amadeusk.liftlog.data

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "lift log_settings"
private const val KEY_USE_KG = "useKg"
private const val KEY_DARK_THEME = "darkTheme"
private const val KEY_REMINDERS_ENABLED = "remindersEnabled"
private const val KEY_AGGRESSIVE_REMINDERS = "aggressiveReminders"

fun loadUseKg(context: Context, default: Boolean = true): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_USE_KG, default)
}

fun saveUseKg(context: Context, useKg: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit() { putBoolean(KEY_USE_KG, useKg) }
}

fun loadDarkTheme(context: Context, default: Boolean): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_DARK_THEME, default)
}

fun saveDarkTheme(context: Context, darkTheme: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit() { putBoolean(KEY_DARK_THEME, darkTheme) }
}

fun loadReminderEnabled(context: Context, default: Boolean = false): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_REMINDERS_ENABLED, default)
}

fun saveReminderEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit() { putBoolean(KEY_REMINDERS_ENABLED, enabled) }
}

fun loadAggressiveRemindersEnabled(context: Context, default: Boolean = false): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_AGGRESSIVE_REMINDERS, default)
}

fun saveAggressiveRemindersEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit() { putBoolean(KEY_AGGRESSIVE_REMINDERS, enabled) }
}

