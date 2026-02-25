package com.amadeusk.liftlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    var weight by remember { mutableStateOf(initialWeight) }
    var date by remember { mutableStateOf(initialDate) }

    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    val isDateValid = remember(date) {
        runCatching { LocalDate.parse(date.trim(), formatter) }.isSuccess
    }

    val weightVal = remember(weight) { weight.trim().toDoubleOrNull() }
    val isWeightValid = weightVal != null && weightVal > 0.0

    val canSave = isDateValid && isWeightValid

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onConfirm(weight.trim(), date.trim()) }
            ) { Text(confirmButtonText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                DateTextFieldWithCalendar(
                    label = "Date",
                    dateText = date,
                    onDateTextChange = { date = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

