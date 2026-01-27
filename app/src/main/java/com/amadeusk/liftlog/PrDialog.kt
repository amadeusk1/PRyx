package com.amadeusk.liftlog

// Android date picker dialog
import android.app.DatePickerDialog

// Compose layout + interaction
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth

// Keyboard config for numeric fields
import androidx.compose.foundation.text.KeyboardOptions

// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange

// Material UI components
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

// Compose runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// UI helpers
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Date utilities
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Dialog for adding/editing a PR (exercise + weight + reps + date)
@Composable
fun PrDialog(
    title: String,
    confirmButtonText: String,
    initialExercise: String,
    initialWeight: String,
    initialReps: String,
    initialDate: String,
    useKg: Boolean,                       // Controls displayed unit label
    exerciseSuggestions: List<String>,     // Suggestions from previous exercises
    onDismiss: () -> Unit,
    onConfirm: (exercise: String, weightStr: String, repsStr: String, date: String) -> Unit
) {
    // Always include core lifts at the top of suggestions
    val coreLifts = listOf("Bench Press", "Squat", "Deadlift")

    // Merge core lifts + past exercises (remove duplicates)
    val allSuggestions = (coreLifts + exerciseSuggestions).distinct()

    // Selected dropdown exercise (only used if custom field is empty)
    var selectedExercise by remember {
        mutableStateOf(if (initialExercise in allSuggestions) initialExercise else "")
    }

    // Custom exercise text field (overrides dropdown if filled)
    var customExerciseText by remember {
        mutableStateOf(if (initialExercise in allSuggestions) "" else initialExercise)
    }

    // Input text state
    var weightText by remember { mutableStateOf(initialWeight) }
    var repsText by remember { mutableStateOf(initialReps) }
    var dateText by remember { mutableStateOf(initialDate) }

    // Dropdown open/close state
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Label for weight input depending on units
    val weightLabel = if (useKg) "Weight (kg)" else "Weight (lb)"

    // Expected date format: yyyy-MM-dd
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    // ----- Validation -----

    // Date must parse successfully
    val isDateValid = remember(dateText) {
        runCatching { LocalDate.parse(dateText.trim(), formatter) }.isSuccess
    }

    // Weight must be numeric and > 0
    val weightValue = remember(weightText) { weightText.trim().toDoubleOrNull() }

    // Reps must be integer and > 0
    val repsValue = remember(repsText) { repsText.trim().toIntOrNull() }

    val isWeightValid = weightValue != null && weightValue > 0.0
    val isRepsValid = repsValue != null && repsValue > 0

    // Enable save only when everything is valid
    val canSave = isDateValid && isWeightValid && isRepsValid

    AlertDialog(
        onDismissRequest = onDismiss,

        // Save button (disabled if inputs invalid)
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    // Use custom exercise if typed, otherwise use dropdown selection
                    val finalExercise =
                        if (customExerciseText.isNotBlank()) customExerciseText.trim()
                        else selectedExercise.trim()

                    // Return values back to caller
                    onConfirm(
                        finalExercise,
                        weightText.trim(),
                        repsText.trim(),
                        dateText.trim()
                    )
                }
            ) { Text(confirmButtonText) }
        },

        // Cancel button
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },

        title = { Text(title) },

        // Dialog content (form fields)
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // ---------- Exercise dropdown ----------
                Box(modifier = Modifier.fillMaxWidth()) {

                    // Read-only field that opens dropdown when tapped
                    OutlinedTextField(
                        value = selectedExercise,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exercise (from list)") },
                        supportingText = { Text("Tap to pick a common or previous exercise") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true },
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "Open exercise list"
                                )
                            }
                        }
                    )

                    // Dropdown menu with suggestions
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    selectedExercise = suggestion
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // ---------- Custom exercise ----------
                OutlinedTextField(
                    value = customExerciseText,
                    onValueChange = { customExerciseText = it },
                    label = { Text("Or custom exercise") },
                    supportingText = { Text("Overrides list selection if filled") },
                    modifier = Modifier.fillMaxWidth()
                )

                // ---------- Weight ----------
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(weightLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,

                    // Show error if user typed something invalid
                    isError = weightText.isNotBlank() && !isWeightValid,

                    // Helper text under the field
                    supportingText = {
                        when {
                            weightText.isBlank() -> Text("Required")
                            !isWeightValid -> Text("Enter a number > 0")
                            else -> Text("")
                        }
                    },

                    // Numeric keyboard (decimals usually supported)
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                // ---------- Reps ----------
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    label = { Text("Reps") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = repsText.isNotBlank() && !isRepsValid,
                    supportingText = {
                        when {
                            repsText.isBlank() -> Text("Required")
                            !isRepsValid -> Text("Enter a whole number > 0")
                            else -> Text("")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                // ---------- Date ----------
                DateTextFieldWithCalendar(
                    label = "Date",
                    dateText = dateText,
                    onDateTextChange = { dateText = it }
                )
            }
        }
    )
}

// Date input field with typing + calendar picker
@Composable
private fun DateTextFieldWithCalendar(
    label: String,
    dateText: String,
    onDateTextChange: (String) -> Unit
) {
    val context = LocalContext.current

    // Expected date format: yyyy-MM-dd
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    // Picker default date (parse input if possible, otherwise use today)
    val initialDate = remember(dateText) {
        runCatching { LocalDate.parse(dateText.trim(), formatter) }
            .getOrElse { LocalDate.now(ZoneId.systemDefault()) }
    }

    // Validate typed date
    val isValid = remember(dateText) {
        runCatching { LocalDate.parse(dateText.trim(), formatter) }.isSuccess
    }

    OutlinedTextField(
        value = dateText,
        onValueChange = onDateTextChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,

        // Error styling when invalid
        isError = dateText.isNotBlank() && !isValid,

        // Helper text under the field
        supportingText = {
            when {
                dateText.isBlank() -> Text("Required (YYYY-MM-DD)")
                !isValid -> Text("Use format YYYY-MM-DD")
                else -> Text("")
            }
        },

        // Numeric keyboard (keeps input simple for YYYY-MM-DD typing)
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),

        // Calendar icon opens DatePickerDialog
        trailingIcon = {
            IconButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            // month is 0-based in DatePickerDialog
                            val picked = LocalDate.of(year, month + 1, day)
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
