package com.meneses.budgethunter.commons.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maxkeppeker.sheets.core.models.base.rememberSheetState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.meneses.budgethunter.R
import com.meneses.budgethunter.commons.EMPTY

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun DateField(
    date: String?,
    label: String,
    onDateSelected: (String) -> Unit
) {
    val calendarState = rememberSheetState()

    CalendarDialog(
        state = calendarState,
        config = CalendarConfig(
            monthSelection = true,
            yearSelection = true
        ),
        selection = CalendarSelection.Date {
            onDateSelected(it.toString())
        }
    )

    ExposedDropdownMenuBox(
        expanded = false,
        onExpandedChange = {
            if (it) calendarState.show()
        }
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(),
            value = date ?: EMPTY,
            readOnly = true,
            label = { Text(text = label) },
            onValueChange = {},
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.entry_date)
                )
            },
            singleLine = true
        )
    }
}
