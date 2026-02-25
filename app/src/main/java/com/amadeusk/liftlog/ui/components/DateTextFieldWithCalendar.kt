package com.amadeusk.liftlog.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DateTextFieldWithCalendar(
    label: String,
    dateText: String,
    onDateTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    val initialDate = remember(dateText) {
        runCatching { LocalDate.parse(dateText.trim(), formatter) }
            .getOrElse { LocalDate.now(ZoneId.systemDefault()) }
    }

    val isValid = remember(dateText) {
        dateText.isNotBlank() &&
                runCatching { LocalDate.parse(dateText.trim(), formatter) }.isSuccess
    }

    OutlinedTextField(
        value = dateText,
        onValueChange = { onDateTextChange(it) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        isError = dateText.isNotBlank() && !isValid,
        supportingText = {
            when {
                dateText.isBlank() -> Text("Required (YYYY-MM-DD)")
                !isValid -> Text("Use format YYYY-MM-DD")
                else -> Text("")
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            IconButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
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

