package com.amadeusk.liftlog.data

import android.content.Context
import androidx.core.content.edit

enum class DashboardSection(val id: String, val label: String) {
    DAILY_QUOTE("daily_quote", "Daily quote"),
    STREAK("streak", "Daily streak"),
    LIFTS("lifts", "Lifts"),
    BODYWEIGHT("bodyweight", "Bodyweight"),
    TOOLS("tools", "Tools"),
    THIS_WEEK("this_week", "This week"),
    LEADERBOARD("leaderboard", "Leaderboard");

    companion object {
        fun fromId(id: String): DashboardSection? = entries.find { it.id == id }
    }
}

data class DashboardSectionItem(
    val section: DashboardSection,
    val enabled: Boolean
)

data class HomeLiftItem(
    val name: String,
    val enabled: Boolean
)

val DEFAULT_HOME_LIFTS = listOf("Bench Press", "Squat", "Deadlift")

private const val PREFS_NAME = "lift log_settings"
private const val KEY_DASHBOARD_LAYOUT = "dashboardLayout"
private const val KEY_HOME_LIFTS = "homeLifts"
private const val KEY_HOME_LIFT_LAYOUT = "homeLiftLayout"

fun defaultDashboardLayout(): List<DashboardSectionItem> =
    DashboardSection.entries.map { DashboardSectionItem(it, enabled = true) }

fun loadDashboardLayout(context: Context): List<DashboardSectionItem> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val stored = prefs.getString(KEY_DASHBOARD_LAYOUT, null) ?: return defaultDashboardLayout()
    return parseDashboardLayout(stored)
}

private fun parseDashboardLayout(stored: String): List<DashboardSectionItem> {
    val parsed = stored.split(';')
        .mapNotNull { part ->
            val kv = part.split(':')
            if (kv.size != 2) return@mapNotNull null
            val section = DashboardSection.fromId(kv[0]) ?: return@mapNotNull null
            DashboardSectionItem(section, kv[1] == "1")
        }
    if (parsed.isEmpty()) return defaultDashboardLayout()

    val known = parsed.map { it.section }.toSet()
    val merged = parsed.toMutableList()
    for (section in DashboardSection.entries) {
        if (section !in known) {
            merged.add(DashboardSectionItem(section, enabled = true))
        }
    }
    return merged
}

fun saveDashboardLayout(context: Context, layout: List<DashboardSectionItem>) {
    val encoded = layout.joinToString(";") { "${it.section.id}:${if (it.enabled) 1 else 0}" }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(KEY_DASHBOARD_LAYOUT, encoded)
    }
}

fun defaultHomeLiftLayout(): List<HomeLiftItem> =
    DEFAULT_HOME_LIFTS.map { HomeLiftItem(it, enabled = true) }

fun homeLiftsFromLayout(layout: List<HomeLiftItem>): List<String> =
    layout.filter { it.enabled }.map { it.name }

fun loadHomeLiftLayout(context: Context, loggedExercises: List<String>): List<HomeLiftItem> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val stored = prefs.getString(KEY_HOME_LIFT_LAYOUT, null)
    val layout = if (stored.isNullOrBlank()) {
        migrateHomeLiftLayoutFromLegacy(context, loggedExercises)
    } else {
        parseHomeLiftLayout(stored)
    }
    return mergeHomeLiftLayout(layout, loggedExercises)
}

private fun migrateHomeLiftLayoutFromLegacy(
    context: Context,
    loggedExercises: List<String>
): List<HomeLiftItem> {
    val legacyEnabled = loadHomeLiftsLegacy(context)
    val available = availableHomeLiftOptions(loggedExercises, legacyEnabled)
    val enabledSet = legacyEnabled.toSet()
    val ordered = legacyEnabled.map { HomeLiftItem(it, enabled = true) }
    val rest = available
        .filter { it !in enabledSet }
        .map { HomeLiftItem(it, enabled = false) }
    return ordered + rest
}

private fun loadHomeLiftsLegacy(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val stored = prefs.getString(KEY_HOME_LIFTS, null)
    if (stored.isNullOrBlank()) return DEFAULT_HOME_LIFTS
    return stored.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}

private fun parseHomeLiftLayout(stored: String): List<HomeLiftItem> {
    return stored.split(';')
        .mapNotNull { part ->
            val sep = part.lastIndexOf(':')
            if (sep <= 0) return@mapNotNull null
            val name = part.substring(0, sep)
            val enabled = part.substring(sep + 1) == "1"
            if (name.isBlank()) return@mapNotNull null
            HomeLiftItem(name, enabled)
        }
}

fun mergeHomeLiftLayout(
    current: List<HomeLiftItem>,
    loggedExercises: List<String>
): List<HomeLiftItem> {
    val availableNames = availableHomeLiftOptions(loggedExercises, current.map { it.name })
    val byName = current.associateBy { it.name }
    val ordered = current.filter { it.name in availableNames }
    val newOnes = availableNames
        .filter { it !in byName }
        .map { HomeLiftItem(it, enabled = false) }
    return ordered + newOnes
}

fun saveHomeLiftLayout(context: Context, layout: List<HomeLiftItem>) {
    val encoded = layout.joinToString(";") { item ->
        "${item.name}:${if (item.enabled) 1 else 0}"
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(KEY_HOME_LIFT_LAYOUT, encoded)
        putString(KEY_HOME_LIFTS, homeLiftsFromLayout(layout).joinToString(","))
    }
}

fun loadHomeLifts(context: Context): List<String> {
    return homeLiftsFromLayout(loadHomeLiftLayout(context, emptyList()))
}

fun saveHomeLifts(context: Context, lifts: List<String>) {
    val enabledSet = lifts.toSet()
    val layout = lifts.map { HomeLiftItem(it, enabled = true) } +
        DEFAULT_HOME_LIFTS.filter { it !in enabledSet }.map { HomeLiftItem(it, enabled = false) }
    saveHomeLiftLayout(context, layout)
}

fun availableHomeLiftOptions(
    loggedExercises: List<String>,
    selectedLifts: List<String> = emptyList()
): List<String> {
    val extras = (loggedExercises + selectedLifts)
        .filter { it !in DEFAULT_HOME_LIFTS }
        .distinct()
        .sorted()
    return DEFAULT_HOME_LIFTS + extras
}
