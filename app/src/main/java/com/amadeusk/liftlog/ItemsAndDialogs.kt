package com.amadeusk.liftlog

// Android date picker dialog
import android.app.DatePickerDialog

// Compose layouts + Material UI
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Keyboard options for text fields
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// App data models
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.PR

// Date utilities
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ---------- Selectors & Range ----------

// Dropdown selector to choose an exercise for graphs / filtering
@Composable
fun ExerciseSelector(
    exercises: List<String>,                 // All exercise names
    selectedExercise: String?,               // Currently selected exercise
    onExerciseSelected: (String) -> Unit     // Callback when user picks one
) {
    // Controls whether the dropdown is open
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Select exercise", style = MaterialTheme.typography.labelMedium)

        // Button that opens the dropdown
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedExercise ?: "Choose exercise")
        }

        // Dropdown menu list of exercises
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise) },
                    onClick = {
                        // Send selection up and close menu
                        onExerciseSelected(exercise)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Buttons for selecting graph time range (month/year/all)
@Composable
fun GraphRangeSelector(
    selectedRange: GraphRange,                  // Current range
    onRangeSelected: (GraphRange) -> Unit       // Callback when range changes
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

// Single range button with a "selected" style
@Composable
private fun RangeButton(
    label: String,
    range: GraphRange,
    selectedRange: GraphRange,
    onRangeSelected: (GraphRange) -> Unit
) {
    // True if this button is the active range
    val selected = selectedRange == range

    OutlinedButton(
        onClick = { onRangeSelected(range) },
        // Apply a subtle background when selected
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

// ---------- List Items ----------

// Card row for displaying a PR item (with edit/delete)
@Composable
fun PRItem(
    pr: PR,                 // PR being displayed
    useKg: Boolean,          // Unit setting
    onDelete: () -> Unit,    // Delete callback
    onEdit: () -> Unit       // Edit callback
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = pr.exercise, style = MaterialTheme.typography.titleMedium)
                Text(text = "${formatWeight(pr.weight, useKg)} x ${pr.reps} reps")
                Text(text = pr.date, style = MaterialTheme.typography.bodySmall)
            }

            // Action buttons
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

// Card row for displaying a bodyweight entry (with edit/delete)
@Composable
fun BodyWeightItem(
    entry: BodyWeightEntry,  // Entry being displayed
    useKg: Boolean,           // Unit setting
    onEdit: () -> Unit,       // Edit callback
    onDelete: () -> Unit      // Delete callback
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bodyweight", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${formatWeight(entry.weight, useKg)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(text = entry.date, style = MaterialTheme.typography.bodySmall)
            }

            // Action buttons
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

// ---------- Date Field (Manual typing + Calendar Picker) ----------

// Date input field that supports typing AND picking from a calendar dialog
@Composable
private fun DateTextFieldWithCalendar(
    label: String,
    dateText: String,                         // Current date string in text field
    onDateTextChange: (String) -> Unit,       // Update callback
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // ISO format used everywhere: yyyy-MM-dd
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    // If the current text is invalid, fall back to today's date for the picker
    val initialDate = remember(dateText) {
        runCatching { LocalDate.parse(dateText.trim(), formatter) }
            .getOrElse { LocalDate.now(ZoneId.systemDefault()) }
    }

    // Validate typed input
    val isValid = remember(dateText) {
        dateText.isNotBlank() &&
                runCatching { LocalDate.parse(dateText.trim(), formatter) }.isSuccess
    }

    OutlinedTextField(
        value = dateText,
        onValueChange = { onDateTextChange(it) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,

        // Show error styling when user typed something invalid
        isError = dateText.isNotBlank() && !isValid,

        // Helper / error message under the field
        supportingText = {
            when {
                dateText.isBlank() -> Text("Required (YYYY-MM-DD)")
                !isValid -> Text("Use format YYYY-MM-DD")
                else -> Text("")
            }
        },

        // Keyboard setup (numeric-ish typing + done action)
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),

        // Calendar icon that opens a DatePickerDialog
        trailingIcon = {
            IconButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            // month is 0-based in DatePickerDialog
                            val picked = LocalDate.of(year, month + 1, dayOfMonth)
                            onDateTextChange(picked.format(formatter))
                        },
                        initialDate.year,
                        initialDate.monthValue - 1,
                        initialDate.dayOfMonth
                    ).show()
                }
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = "Pick date")
            }
        }
    )
}

// ---------- Dialogs ----------

// Dialog used for adding/editing a PR record
@Composable
fun PrDialog(
    title: String,
    confirmButtonText: String,
    initialExercise: String,
    initialWeight: String,
    initialReps: String,
    initialDate: String,
    useKg: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (exercise: String, weight: String, reps: String, date: String) -> Unit
) {
    // Local editable state for input fields
    var exercise by remember { mutableStateOf(initialExercise) }
    var weight by remember { mutableStateOf(initialWeight) }
    var reps by remember { mutableStateOf(initialReps) }
    var date by remember { mutableStateOf(initialDate) }

    // ---------- validation ----------
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    // Date must parse correctly
    val isDateValid = remember(date) {
        runCatching { LocalDate.parse(date.trim(), formatter) }.isSuccess
    }

    // Weight must be numeric and > 0
    val weightVal = remember(weight) { weight.trim().toDoubleOrNull() }
    val isWeightValid = weightVal != null && weightVal > 0.0

    // Reps must be whole number and > 0
    val repsVal = remember(reps) { reps.trim().toIntOrNull() }
    val isRepsValid = repsVal != null && repsVal > 0

    // Only allow saving if everything is valid
    val canSave = isDateValid && isWeightValid && isRepsValid

    AlertDialog(
        onDismissRequest = onDismiss,

        // Confirm button (disabled until valid)
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onConfirm(exercise, weight, reps, date) }
            ) {
                Text(confirmButtonText)
            }
        },

        // Cancel button
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },

        title = { Text(title) },

        // Main dialog form content
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Exercise name input
                OutlinedTextField(
                    value = exercise,
                    onValueChange = { exercise = it },
                    label = { Text("Exercise (e.g. Bench Press)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Weight input + validation message
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (${if (useKg) "kg" else "lb"})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = weight.isNotBlank() && !isWeightValid,
                    supportingText = {
                        when {
                            weight.isBlank() -> Text("Required")
                            !isWeightValid -> Text("Enter a number > 0")
                            else -> Text("")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Reps input + validation message
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = reps.isNotBlank() && !isRepsValid,
                    supportingText = {
                        when {
                            reps.isBlank() -> Text("Required")
                            !isRepsValid -> Text("Enter a whole number > 0")
                            else -> Text("")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Date input (typing + calendar picker)
                DateTextFieldWithCalendar(
                    label = "Date",
                    dateText = date,
                    onDateTextChange = { date = it }
                )
            }
        }
    )
}

// Dialog used for adding/editing a bodyweight entry
@Composable
fun BodyWeightDialog(
    title: String,
    confirmButtonText: String,
    initialWeight: String,
    initialDate: String,
    useKg: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (weight: String, date: String) -> Unit
) {
    // Local editable state for input fields
    var weight by remember { mutableStateOf(initialWeight) }
    var date by remember { mutableStateOf(initialDate) }

    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    // Date must parse correctly
    val isDateValid = remember(date) {
        runCatching { LocalDate.parse(date.trim(), formatter) }.isSuccess
    }

    // Weight must be numeric and > 0
    val weightVal = remember(weight) { weight.trim().toDoubleOrNull() }
    val isWeightValid = weightVal != null && weightVal > 0.0

    // Only allow saving if everything is valid
    val canSave = isDateValid && isWeightValid

    AlertDialog(
        onDismissRequest = onDismiss,

        // Confirm button (disabled until valid)
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onConfirm(weight.trim(), date.trim()) }
            ) {
                Text(confirmButtonText)
            }
        },

        // Cancel button
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },

        title = { Text(title) },

        // Main dialog form content
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Weight input + validation message
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (${if (useKg) "kg" else "lb"})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = weight.isNotBlank() && !isWeightValid,
                    supportingText = {
                        when {
                            weight.isBlank() -> Text("Required")
                            !isWeightValid -> Text("Enter a number > 0")
                            else -> Text("")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                // Date input (typing + calendar picker)
                DateTextFieldWithCalendar(
                    label = "Date",
                    dateText = date,
                    onDateTextChange = { date = it }
                )
            }
        }
    )
}
