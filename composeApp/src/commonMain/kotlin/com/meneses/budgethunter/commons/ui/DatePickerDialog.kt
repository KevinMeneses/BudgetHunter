package com.meneses.budgethunter.commons.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.cancel
import budgethunter.composeapp.generated.resources.ok
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    showDialog: Boolean,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    initialDate: String? = null
) {
    if (showDialog) {
        val initialDateMillis = remember(initialDate) {
            if (initialDate != null) {
                try {
                    val parts = initialDate.split("-")
                    if (parts.size == 3) {
                        val year = parts[0].toInt()
                        val month = parts[1].toInt()
                        val day = parts[2].toInt()
                        val localDate = LocalDate(year, month, day)
                        localDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis
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
                            val instant = Instant.fromEpochMilliseconds(millis)
                            val localDateTime = instant.toLocalDateTime(TimeZone.UTC)
                            val year = localDateTime.year
                            val month = localDateTime.monthNumber
                            val day = localDateTime.dayOfMonth
                            val formattedDate = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                            onDateSelected(formattedDate)
                        }
                        onDismiss()
                    },
                    enabled = confirmEnabled.value
                ) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Asks for a range of days, both ends included. Confirming needs both ends.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onRangeSelected: (start: LocalDate, end: LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialStart: LocalDate? = null,
    initialEnd: LocalDate? = null
) {
    // The picker works with UTC midnights
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        initialSelectedEndDateMillis = initialEnd?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        onRangeSelected(start.toUtcDate(), end.toUtcDate())
                    }
                    onDismiss()
                },
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    ) {
        // The default headline ("Start date - End date" with large text) does not fit the
        // dialog's width and wraps letter by letter; show the chosen days compactly instead
        DateRangePicker(
            state = state,
            modifier = Modifier.weight(1f),
            title = null,
            headline = {
                val start = state.selectedStartDateMillis?.toUtcDate()?.toDayText() ?: "–"
                val end = state.selectedEndDateMillis?.toUtcDate()?.toDayText() ?: "–"
                Text(
                    text = "$start  →  $end",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)
                )
            },
            showModeToggle = false
        )
    }
}

/** "5/10/2026" */
private fun LocalDate.toDayText(): String = "$dayOfMonth/$monthNumber/$year"

private fun Long.toUtcDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
