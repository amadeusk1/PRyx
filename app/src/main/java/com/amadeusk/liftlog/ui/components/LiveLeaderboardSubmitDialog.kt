package com.amadeusk.liftlog.ui.components

import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.LiveLeaderboardViewModel

// Display label -> API value (exercise must be exactly "bench", "deadlift", or "squat")
private val EXERCISES = listOf(
    "Bench Press" to "bench",
    "Squat" to "squat",
    "Deadlift" to "deadlift"
)

@Composable
fun LiveLeaderboardSubmitDialog(
    viewModel: LiveLeaderboardViewModel,
    useKg: Boolean,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var selectedExerciseApi by remember { mutableStateOf<String?>(null) } // "bench" | "squat" | "deadlift"
    var weightInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }
    var proofImageBase64 by remember { mutableStateOf<String?>(null) }
    var proofVideoBase64 by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val proofPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val base64 = Base64.encodeToString(stream.readBytes(), Base64.NO_WRAP)
                when {
                    mimeType.startsWith("image/") -> {
                        proofImageBase64 = base64
                        proofVideoBase64 = null
                    }
                    mimeType.startsWith("video/") -> {
                        proofVideoBase64 = base64
                        proofImageBase64 = null
                    }
                    else -> {
                        proofImageBase64 = base64
                        proofVideoBase64 = null
                    }
                }
            }
        }
    }

    val weightValue = remember(weightInput) { weightInput.trim().toDoubleOrNull() }
    val weightValid = weightValue != null && weightValue > 0.0
    val repsValue = remember(repsInput) { repsInput.trim().toIntOrNull()?.takeIf { it > 0 } }

    val canSubmit = name.isNotBlank() &&
        selectedExerciseApi != null &&
        weightValid &&
        repsValue != null &&
        !uiState.isSubmitting

    val weightLabel = if (useKg) "Weight (kg)" else "Weight (lb)"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = {
                    viewModel.submit(
                        name = name,
                        exercise = selectedExerciseApi!!,
                        weightDisplay = weightValue!!,
                        useKg = useKg,
                        notes = notes,
                        reps = repsValue!!,
                        imageBase64 = proofImageBase64,
                        videoBase64 = proofVideoBase64
                    )
                    onDismiss()
                }
            ) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Submit to live leaderboard") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = EXERCISES.find { it.second == selectedExerciseApi }?.first ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exercise") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true },
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select exercise")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EXERCISES.forEach { (display, apiValue) ->
                            DropdownMenuItem(
                                text = { Text(display) },
                                onClick = {
                                    selectedExerciseApi = apiValue
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(weightLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = weightInput.isNotBlank() && !weightValid,
                    supportingText = {
                        Text("Use the same unit (kg or lb) as set in app Settings.")
                    }
                )

                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Reps") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = repsInput.isNotBlank() && repsValue == null,
                    supportingText = {
                        if (repsInput.isNotBlank() && repsValue == null) Text("Enter a number > 0")
                    }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Text(
                    text = "Image or video proof is optional but more likely to be accepted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { proofPicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            proofVideoBase64 != null -> "Video attached"
                            proofImageBase64 != null -> "Image attached"
                            else -> "Attach image or video (optional)"
                        }
                    )
                }
            }
        }
    )
}
