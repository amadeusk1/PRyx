package com.amadeusk.liftlog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.PR
import kotlin.math.roundToInt

@Composable
fun ExerciseGraph(
    prs: List<PR>,
    selectedPr: PR?,
    onPointSelected: (PR) -> Unit,
    useKg: Boolean,
    modifier: Modifier = Modifier
) {
    val sorted = remember(prs) {
        prs.sortedWith(compareBy<PR> { parsePrDateOrMin(it.date) }.thenBy { it.id })
    }

    ProfessionalLineChart(
        title = "PR Progress",
        items = sorted,
        selected = selectedPr,
        onSelected = onPointSelected,
        // convert ONCE for chart scaling
        getValue = { it.weight.toDisplayWeight(useKg) },
        getLabel = { shortDateLabel(it.date) },
        // v is already in display units — DO NOT convert again
        formatValue = { v ->
            "${v.roundToInt()} ${if (useKg) "kg" else "lb"}"
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun BodyWeightGraph(
    entries: List<BodyWeightEntry>,
    selectedEntry: BodyWeightEntry?,
    onPointSelected: (BodyWeightEntry) -> Unit,
    useKg: Boolean,
    modifier: Modifier = Modifier
) {
    val sorted = remember(entries) {
        entries.sortedWith(compareBy<BodyWeightEntry> { parsePrDateOrMin(it.date) }.thenBy { it.id })
    }

    ProfessionalLineChart(
        title = "Bodyweight Progress",
        items = sorted,
        selected = selectedEntry,
        onSelected = onPointSelected,
        // convert ONCE for chart scaling
        getValue = { it.weight.toDisplayWeight(useKg) },
        getLabel = { shortDateLabel(it.date) },
        // v is already in display units — DO NOT convert again
        formatValue = { v ->
            "${v.roundToInt()} ${if (useKg) "kg" else "lb"}"
        },
        modifier = modifier.fillMaxWidth()
    )
}

private fun shortDateLabel(date: String): String {
    return if (date.length >= 10 && date[4] == '-' && date[7] == '-') {
        val mm = date.substring(5, 7)
        val dd = date.substring(8, 10)
        "$mm/$dd"
    } else date
}
