package com.amadeusk.liftlog.util

import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.PR
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

// ---- Daily quote + streak helpers ----

data class DailyQuote(val text: String, val author: String)

private val DAILY_QUOTES: List<DailyQuote> = listOf(
    DailyQuote(
        text = "Whatever it takes. No excuses.",
        author = "Rich Piana"
    ),
    DailyQuote(
        text = "Discipline over motivation. Show up anyway.",
        author = "Sam Sulek"
    ),
    DailyQuote(
        text = "Champions do extra when nobody is watching.",
        author = "Chris Bumstead"
    ),
    DailyQuote(
        text = "The mind is the most important thing. It’s the mind that builds the body.",
        author = "Arnold Schwarzenegger"
    ),
    DailyQuote(
        text = "The worst thing I can be is the same as everybody else. I hate that.",
        author = "Arnold Schwarzenegger"
    ),
    DailyQuote(
        text = "If you want something you’ve never had, you must be willing to do something you’ve never done.",
        author = "Ronnie Coleman"
    ),
    DailyQuote(
        text = "Yeah buddy! Lightweight baby!",
        author = "Ronnie Coleman"
    ),
    DailyQuote(
        text = "Everybody wants to be a bodybuilder, but nobody wants to lift no heavy-*** weights.",
        author = "Ronnie Coleman"
    ),
    DailyQuote(
        text = "Hard work and dedication.",
        author = "Dorian Yates"
    ),
    DailyQuote(
        text = "Suffering is the test of life.",
        author = "Dorian Yates"
    ),
    DailyQuote(
        text = "Intensity builds immensity.",
        author = "Kevin Levrone"
    ),
    DailyQuote(
        text = "A champion is someone who gets up when they can’t.",
        author = "Jack Dempsey"
    ),
    DailyQuote(
        text = "Strength does not come from winning. Your struggles develop your strengths.",
        author = "Arnold Schwarzenegger"
    ),
    DailyQuote(
        text = "Small progress is still progress.",
        author = "LiftLog"
    ),
    DailyQuote(
        text = "Consistency beats occasional intensity.",
        author = "LiftLog"
    ),
    DailyQuote(
        text = "Train with intent. Recover with discipline.",
        author = "LiftLog"
    ),
    DailyQuote(
        text = "Strength is earned one rep at a time.",
        author = "Unknown lifter"
    ),
    DailyQuote(
        text = "You don’t rise to the occasion, you fall to your training.",
        author = "Powerlifting proverb"
    ),
    DailyQuote(
        text = "Future you is built by what you log today.",
        author = "LiftLog"
    )
)

fun getDailyQuote(today: LocalDate = LocalDate.now()): DailyQuote {
    if (DAILY_QUOTES.isEmpty()) {
        return DailyQuote(
            text = "Stay consistent. Strength will follow.",
            author = "LiftLog"
        )
    }
    val idx = (today.toEpochDay().toInt().mod(DAILY_QUOTES.size) + DAILY_QUOTES.size) % DAILY_QUOTES.size
    return DAILY_QUOTES[idx]
}

// Count consecutive days (including today) with at least one PR or bodyweight entry
fun currentActivityStreak(
    prs: List<PR>,
    bodyWeights: List<BodyWeightEntry>,
    today: LocalDate = LocalDate.now()
): Int {
    if (prs.isEmpty() && bodyWeights.isEmpty()) return 0

    val dates = mutableSetOf<LocalDate>()

    prs.forEach { pr ->
        runCatching { LocalDate.parse(pr.date, prDateFormatter) }
            .getOrNull()
            ?.let { dates.add(it) }
    }

    bodyWeights.forEach { bw ->
        runCatching { LocalDate.parse(bw.date, prDateFormatter) }
            .getOrNull()
            ?.let { dates.add(it) }
    }

    var streak = 0
    var day = today
    while (dates.contains(day)) {
        streak += 1
        day = day.minusDays(1)
    }
    return streak
}

// ---- Rep filter for PR graph/history ----
enum class RepRange(val reps: Int?) {
    ONE(1),
    THREE(3),
    SIX(6),
    EIGHT(8),
    ALL(null)
}

fun filterPrsByRepRange(prs: List<PR>, repRange: RepRange): List<PR> {
    val r = repRange.reps ?: return prs
    return prs.filter { it.reps == r }
}

// Shared date formatter (expects yyyy-MM-dd)
val prDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

// Filter PRs to only include entries from the selected time range
fun filterPrsByRange(prs: List<PR>, range: GraphRange): List<PR> {
    if (range == GraphRange.ALL) return prs

    val now = LocalDate.now()
    val startDate = when (range) {
        GraphRange.MONTH -> now.withDayOfMonth(1)
        GraphRange.YEAR -> now.withDayOfYear(1)
        GraphRange.ALL -> LocalDate.MIN
    }

    return prs.filter { pr ->
        val date = try {
            LocalDate.parse(pr.date, prDateFormatter)
        } catch (_: DateTimeParseException) {
            return@filter true
        }
        !date.isBefore(startDate)
    }
}

// Same filter logic for bodyweight entries
fun filterBodyWeightsByRange(
    entries: List<BodyWeightEntry>,
    range: GraphRange
): List<BodyWeightEntry> {
    if (range == GraphRange.ALL) return entries

    val now = LocalDate.now()
    val startDate = when (range) {
        GraphRange.MONTH -> now.withDayOfMonth(1)
        GraphRange.YEAR -> now.withDayOfYear(1)
        GraphRange.ALL -> LocalDate.MIN
    }

    return entries.filter { e ->
        val date = try {
            LocalDate.parse(e.date, prDateFormatter)
        } catch (_: DateTimeParseException) {
            return@filter true
        }
        !date.isBefore(startDate)
    }
}

// Parse a user-entered date string, or return LocalDate.MIN if invalid
fun parsePrDateOrMin(dateStr: String): LocalDate =
    try {
        LocalDate.parse(dateStr, prDateFormatter)
    } catch (_: Exception) {
        LocalDate.MIN
    }

// ---- Unit helpers ----
const val KG_TO_LB = 2.2046226
const val LB_TO_KG = 1.0 / KG_TO_LB

fun Double.toDisplayWeight(useKg: Boolean): Double =
    if (useKg) this else this * KG_TO_LB

fun Double.fromDisplayWeight(useKg: Boolean): Double =
    if (useKg) this else this * LB_TO_KG

fun formatWeight(weightKg: Double, useKg: Boolean): String {
    val value = weightKg.toDisplayWeight(useKg)
    val unit = if (useKg) "kg" else "lb"
    return "${"%.1f".format(value)} $unit"
}

