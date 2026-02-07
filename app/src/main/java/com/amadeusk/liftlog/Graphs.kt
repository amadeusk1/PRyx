package com.amadeusk.liftlog

// Compose runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// UI utilities
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth

// Data models
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.PR

// ---- Formatting helpers ----

// Format an already-converted (display-unit) weight value for graph labels.
// We keep one decimal so lb mode stays accurate (kg->lb conversion commonly yields decimals).
private fun formatGraphWeight(value: Double, useKg: Boolean): String {
    val unit = if (useKg) "kg" else "lb"
    return "${"%.1f".format(value)} $unit"
}

// Graph for exercise PR progression
@Composable
fun ExerciseGraph(
    prs: List<PR>,                     // List of PR data points
    selectedPr: PR?,                   // Currently selected PR (highlighted point)
    onPointSelected: (PR) -> Unit,     // Callback when a point is tapped
    useKg: Boolean,                    // Whether to display values in kg
    modifier: Modifier = Modifier
) {
    // Sort PRs by date, then by ID for stable ordering
    val sorted = remember(prs) {
        prs.sortedWith(
            compareBy<PR> { parsePrDateOrMin(it.date) }
                .thenBy { it.id }
        )
    }

    // Render the line chart
    ProfessionalLineChart(
        title = "PR Progress",
        items = sorted,
        selected = selectedPr,
        onSelected = onPointSelected,

        // Convert weight once for chart scaling
        getValue = { it.weight.toDisplayWeight(useKg) },

        // Format x-axis labels as MM/DD
        getLabel = { shortDateLabel(it.date) },

        // Format value label (already converted)
        formatValue = { v -> formatGraphWeight(v, useKg) },

        modifier = modifier.fillMaxWidth()
    )
}

// Graph for bodyweight progression over time
@Composable
fun BodyWeightGraph(
    entries: List<BodyWeightEntry>,            // List of bodyweight entries
    selectedEntry: BodyWeightEntry?,           // Currently selected entry
    onPointSelected: (BodyWeightEntry) -> Unit,// Callback when a point is tapped
    useKg: Boolean,                            // Whether to display values in kg
    modifier: Modifier = Modifier
) {
    // Sort entries by date, then by ID for consistency
    val sorted = remember(entries) {
        entries.sortedWith(
            compareBy<BodyWeightEntry> { parsePrDateOrMin(it.date) }
                .thenBy { it.id }
        )
    }

    // Render the line chart
    ProfessionalLineChart(
        title = "Bodyweight Progress",
        items = sorted,
        selected = selectedEntry,
        onSelected = onPointSelected,

        // Convert weight once for chart scaling
        getValue = { it.weight.toDisplayWeight(useKg) },

        // Format x-axis labels as MM/DD
        getLabel = { shortDateLabel(it.date) },

        // Format value label (already converted)
        formatValue = { v -> formatGraphWeight(v, useKg) },

        modifier = modifier.fillMaxWidth()
    )
}

// Converts YYYY-MM-DD into MM/DD for graph labels
private fun shortDateLabel(date: String): String {
    return if (date.length >= 10 && date[4] == '-' && date[7] == '-') {
        val mm = date.substring(5, 7)
        val dd = date.substring(8, 10)
        "$mm/$dd"
    } else date // Fallback if format is unexpected
}
