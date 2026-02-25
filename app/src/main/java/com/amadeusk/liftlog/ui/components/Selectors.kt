package com.amadeusk.liftlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.util.GraphRange
import com.amadeusk.liftlog.util.RepRange

@Composable
fun ExerciseSelector(
    exercises: List<String>,
    selectedExercise: String?,
    onExerciseSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Select exercise", style = MaterialTheme.typography.labelMedium)

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedExercise ?: "Choose exercise")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise) },
                    onClick = {
                        onExerciseSelected(exercise)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RepRangeSelector(
    selectedRange: RepRange,
    onRangeSelected: (RepRange) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val label = when (selectedRange) {
        RepRange.ONE -> "1 rep"
        RepRange.THREE -> "3 rep"
        RepRange.SIX -> "6 rep"
        RepRange.EIGHT -> "8 rep"
        RepRange.ALL -> "All reps"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text("Rep range", style = MaterialTheme.typography.labelMedium)

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf(
                "1 rep" to RepRange.ONE,
                "3 rep" to RepRange.THREE,
                "6 rep" to RepRange.SIX,
                "8 rep" to RepRange.EIGHT,
                "All reps" to RepRange.ALL
            ).forEach { (text, value) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onRangeSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GraphRangeSelector(
    selectedRange: GraphRange,
    onRangeSelected: (GraphRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RangeButton("This month", GraphRange.MONTH, selectedRange, onRangeSelected)
        RangeButton("This year", GraphRange.YEAR, selectedRange, onRangeSelected)
        RangeButton("All time", GraphRange.ALL, selectedRange, onRangeSelected)
    }
}

@Composable
private fun RangeButton(
    label: String,
    range: GraphRange,
    selectedRange: GraphRange,
    onRangeSelected: (GraphRange) -> Unit
) {
    val selected = selectedRange == range

    OutlinedButton(
        onClick = { onRangeSelected(range) },
        colors = if (selected) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

