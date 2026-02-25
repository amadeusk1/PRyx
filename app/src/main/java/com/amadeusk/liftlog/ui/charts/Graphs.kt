package com.amadeusk.liftlog.ui.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.util.parsePrDateOrMin
import com.amadeusk.liftlog.util.toDisplayWeight

// Format an already-converted (display-unit) weight value for graph labels.
// We keep one decimal so lb mode stays accurate (kg->lb conversion commonly yields decimals).
private fun formatGraphWeight(value: Double, useKg: Boolean): String {
    val unit = if (useKg) "kg" else "lb"
    return "${"%.1f".format(value)} $unit"
}

@Composable
fun ExerciseGraph(
    prs: List<PR>,
    selectedPr: PR?,
    onPointSelected: (PR) -> Unit,
    useKg: Boolean,
    modifier: Modifier = Modifier,
    showAxisLabels: Boolean = true,
    showTitle: Boolean = true,
    showGrid: Boolean = true,
    lineColor: Color? = null
) {
    val sorted = remember(prs) {
        prs.sortedWith(
            compareBy<PR> { parsePrDateOrMin(it.date) }
                .thenBy { it.id }
        )
    }

    ProfessionalLineChart(
        title = "PR Progress",
        items = sorted,
        selected = selectedPr,
        onSelected = onPointSelected,
        getValue = { it.weight.toDisplayWeight(useKg) },
        getLabel = { shortDateLabel(it.date) },
        formatValue = { v -> formatGraphWeight(v, useKg) },
        modifier = modifier.fillMaxWidth(),
        showAxisLabels = showAxisLabels,
        showTitle = showTitle,
        showGrid = showGrid,
        lineColorOverride = lineColor
    )
}

@Composable
fun BodyWeightGraph(
    entries: List<BodyWeightEntry>,
    selectedEntry: BodyWeightEntry?,
    onPointSelected: (BodyWeightEntry) -> Unit,
    useKg: Boolean,
    modifier: Modifier = Modifier,
    showAxisLabels: Boolean = true,
    showTitle: Boolean = true,
    showGrid: Boolean = true,
    lineColor: Color? = null
) {
    val sorted = remember(entries) {
        entries.sortedWith(
            compareBy<BodyWeightEntry> { parsePrDateOrMin(it.date) }
                .thenBy { it.id }
        )
    }

    ProfessionalLineChart(
        title = "Bodyweight Progress",
        items = sorted,
        selected = selectedEntry,
        onSelected = onPointSelected,
        getValue = { it.weight.toDisplayWeight(useKg) },
        getLabel = { shortDateLabel(it.date) },
        formatValue = { v -> formatGraphWeight(v, useKg) },
        modifier = modifier.fillMaxWidth(),
        showAxisLabels = showAxisLabels,
        showTitle = showTitle,
        showGrid = showGrid,
        lineColorOverride = lineColor
    )
}

private fun shortDateLabel(date: String): String {
    return if (date.length >= 10 && date[4] == '-' && date[7] == '-') {
        val mm = date.substring(5, 7)
        val dd = date.substring(8, 10)
        "$mm/$dd"
    } else date
}

