package com.amadeusk.liftlog

// Data models used by filters
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.BodyWeightEntry

// Date parsing / comparison
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// ---- Tabs for main content ----
enum class LiftLogTab {
    PRS,
    BODYWEIGHT,
    TOOLS
}

// ---- Time range for graph/history ----
enum class GraphRange {
    MONTH,
    YEAR,
    ALL
}

// Shared date formatter (expects yyyy-MM-dd)
val prDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

// Filter PRs to only include entries from the selected time range
fun filterPrsByRange(prs: List<PR>, range: GraphRange): List<PR> {
    // ALL means no filtering
    if (range == GraphRange.ALL) return prs

    val now = LocalDate.now()

    // Start date cutoff (inclusive)
    val startDate = when (range) {
        GraphRange.MONTH -> now.withDayOfMonth(1)   // first day of this month
        GraphRange.YEAR -> now.withDayOfYear(1)     // first day of this year
        GraphRange.ALL -> LocalDate.MIN
    }

    return prs.filter { pr ->
        val date = try {
            // Parse stored date string into LocalDate
            LocalDate.parse(pr.date, prDateFormatter)
        } catch (_: DateTimeParseException) {
            // If parsing fails, keep it so the user's data doesn't disappear
            return@filter true
        }

        // Keep dates that are on/after the start date
        !date.isBefore(startDate)
    }
}

// Same filter logic for bodyweight entries
fun filterBodyWeightsByRange(
    entries: List<BodyWeightEntry>,
    range: GraphRange
): List<BodyWeightEntry> {
    // ALL means no filtering
    if (range == GraphRange.ALL) return entries

    val now = LocalDate.now()

    // Start date cutoff (inclusive)
    val startDate = when (range) {
        GraphRange.MONTH -> now.withDayOfMonth(1)
        GraphRange.YEAR -> now.withDayOfYear(1)
        GraphRange.ALL -> LocalDate.MIN
    }

    return entries.filter { e ->
        val date = try {
            // Parse stored date string into LocalDate
            LocalDate.parse(e.date, prDateFormatter)
        } catch (_: DateTimeParseException) {
            // If parsing fails, keep it so the user's data doesn't disappear
            return@filter true
        }

        // Keep dates that are on/after the start date
        !date.isBefore(startDate)
    }
}

// Parse a user-entered date string, or return LocalDate.MIN if invalid
// (useful for sorting even when some dates are bad)
fun parsePrDateOrMin(dateStr: String): LocalDate =
    try {
        LocalDate.parse(dateStr, prDateFormatter)
    } catch (_: Exception) {
        LocalDate.MIN
    }

// ---- Unit helpers ----

// Conversion constants
const val KG_TO_LB = 2.2046226
const val LB_TO_KG = 1.0 / KG_TO_LB

// Convert stored kg -> display units (kg or lb)
fun Double.toDisplayWeight(useKg: Boolean): Double =
    if (useKg) this else this * KG_TO_LB

// Convert display units (kg or lb) -> stored kg
fun Double.fromDisplayWeight(useKg: Boolean): Double =
    if (useKg) this else this * LB_TO_KG

// Format a weight value stored in kg into a friendly string (e.g. "100.0 kg" or "225.0 lb")
fun formatWeight(weightKg: Double, useKg: Boolean): String {
    val value = weightKg.toDisplayWeight(useKg)
    val unit = if (useKg) "kg" else "lb"
    return "${"%.1f".format(value)} $unit"
}
