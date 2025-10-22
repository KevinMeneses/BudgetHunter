package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate? = null
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    val confirmEnabled = remember {
        derivedStateOf { datePickerState.selectedDateMillis != null }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(selectedDate.toString())
                    }
                    onDismiss()
                },
                enabled = confirmEnabled.value
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Simple calendar dialog for KMP compatibility
 * Uses Material3 components that work across platforms
 */
@Composable
fun SimpleDatePickerDialog(
    showDialog: Boolean,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    initialDate: String? = null
) {
    if (showDialog) {
        val parsedInitialDate = remember(initialDate) {
            try {
                initialDate?.let { LocalDate.parse(it) }
            } catch (e: Exception) {
                null
            }
        }

        BudgetDatePickerDialog(
            onDateSelected = onDateSelected,
            onDismiss = onDismiss,
            initialDate = parsedInitialDate
        )
    }
}

/**
 * Format date for display in text fields - maintains YYYY-MM-dd format
 */
fun formatDateForDisplay(dateString: String?): String {
    return dateString ?: ""
}
