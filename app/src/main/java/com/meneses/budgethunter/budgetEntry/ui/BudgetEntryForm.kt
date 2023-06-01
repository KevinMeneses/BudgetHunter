package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maxkeppeker.sheets.core.models.base.rememberSheetState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.OutlinedDropdown

@Composable
fun BudgetEntryForm(
    budgetEntry: BudgetEntry,
    paddingValues: PaddingValues,
    onBudgetItemChanged: (BudgetEntry) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(all = 20.dp)
    ) {
        AmountField(
            amount = budgetEntry.amount,
            onAmountChanged = {
                onBudgetItemChanged(
                    budgetEntry.copy(amount = it)
                )
            }
        )
        DescriptionField(
            description = budgetEntry.description,
            onDescriptionChanged = {
                onBudgetItemChanged(
                    budgetEntry.copy(description = it)
                )
            }
        )
        TypeSelector(
            type = budgetEntry.type,
            onTypeSelected = {
                onBudgetItemChanged(
                    budgetEntry.copy(type = it)
                )
            }
        )
        DateField(
            date = budgetEntry.date,
            onDateSelected = {
                onBudgetItemChanged(
                    budgetEntry.copy(date = it)
                )
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: String?,
    label: String = stringResource(id = R.string.date),
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

@Composable
fun TypeSelector(
    type: BudgetEntry.Type?,
    onTypeSelected: (BudgetEntry.Type) -> Unit
) {
    val itemTypes = remember { BudgetEntry.getItemTypes() }
    OutlinedDropdown(
        value = type?.toStringResource() ?: EMPTY,
        label = stringResource(id = R.string.type),
        dropdownOptions = itemTypes.map { it.toStringResource() },
        onSelectOption = { onTypeSelected(itemTypes[it]) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountField(
    amount: String,
    onAmountChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = amount,
        onValueChange = {
            if (it.isBlank()) {
                onAmountChanged(EMPTY)
                return@OutlinedTextField
            }
            val decimals = it.split(".").getOrNull(1) ?: EMPTY
            if (decimals == "00") return@OutlinedTextField
            val decimalsLength = decimals.length
            if (decimalsLength > 2) return@OutlinedTextField
            val amountNumber = it.toDoubleOrNull() ?: return@OutlinedTextField
            if (amountNumber > 0) onAmountChanged(it)
        },
        label = { Text(text = stringResource(id = R.string.amount)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescriptionField(
    description: String,
    onDescriptionChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChanged,
        label = { Text(text = stringResource(id = R.string.description)) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        )
    )
}
