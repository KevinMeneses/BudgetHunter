package com.meneses.budgethunter.detail.ui

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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maxkeppeker.sheets.core.models.base.rememberSheetState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.OutlinedDropdown
import com.meneses.budgethunter.insAndOuts.domain.BudgetItem
import java.time.LocalDate

@Composable
fun DetailForm(
    budgetItem: BudgetItem,
    paddingValues: PaddingValues,
    onBudgetItemChanged: (BudgetItem) -> Unit
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
            amount = budgetItem.amount,
            onAmountChanged = {
                onBudgetItemChanged(
                    budgetItem.copy(amount = it)
                )
            }
        )
        DescriptionField(
            description = budgetItem.description,
            onDescriptionChanged = {
                onBudgetItemChanged(
                    budgetItem.copy(description = it)
                )
            }
        )
        TypeSelector(
            type = budgetItem.type,
            onTypeSelected = {
                onBudgetItemChanged(
                    budgetItem.copy(type = it)
                )
            }
        )
        DateField(
            date = budgetItem.date,
            onDateSelected = {
                onBudgetItemChanged(
                    budgetItem.copy(date = it)
                )
            }
        )
        Spacer(
            modifier = Modifier.height(10.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    date: String?,
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
            value = date ?: LocalDate.now().toString(),
            readOnly = true,
            label = { Text(text = "Fecha") },
            onValueChange = {},
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = EMPTY
                )
            },
            singleLine = true
        )
    }
}

@Composable
private fun TypeSelector(
    type: BudgetItem.Type?,
    onTypeSelected: (BudgetItem.Type) -> Unit
) {
    val itemTypes = remember {
        BudgetItem.getItemTypes()
    }
    OutlinedDropdown(
        value = type?.value ?: EMPTY,
        label = "Tipo",
        dropdownOptions = itemTypes.map { it.value },
        onSelectOption = { onTypeSelected(itemTypes[it]) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountField(
    amount: Double?,
    onAmountChanged: (Double) -> Unit
) {
    OutlinedTextField(
        value = amount?.toString() ?: EMPTY,
        onValueChange = {
            onAmountChanged(it.toDoubleOrNull() ?: 0.0)
        },
        label = { Text(text = "Monto") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DescriptionField(
    description: String?,
    onDescriptionChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = description ?: EMPTY,
        onValueChange = onDescriptionChanged,
        label = { Text(text = "Descripcion") },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words
        )
    )
}