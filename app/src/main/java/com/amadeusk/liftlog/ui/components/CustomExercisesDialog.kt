package com.amadeusk.liftlog.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.R

@Composable
fun CustomExercisesDialog(
    customExercises: List<String>,
    onDeleteExercise: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var exerciseToDelete by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = { Text(stringResource(R.string.settings_custom_exercises_section)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.settings_custom_exercises_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (customExercises.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_custom_exercises_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    customExercises.forEach { exercise ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = { exerciseToDelete = exercise }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(
                                        R.string.settings_delete_exercise_content_description
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    )

    exerciseToDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text(stringResource(R.string.settings_delete_exercise_title)) },
            text = {
                Text(stringResource(R.string.settings_delete_exercise_message, exercise))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExercise(exercise)
                        exerciseToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.settings_delete_exercise_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text(stringResource(R.string.settings_delete_exercise_cancel))
                }
            }
        )
    }
}
