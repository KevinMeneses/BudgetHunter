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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maxkeppeker.sheets.core.models.base.rememberSheetState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.meneses.budgethunter.insAndOuts.domain.BudgetItem
import com.meneses.budgethunter.budgetItemTypeList
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.OutlinedDropdown

@Composable
fun DetailForm(
    budgetItem: BudgetItem?,
    paddingValues: PaddingValues
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(all = 20.dp)
    ) {
        AmountField(budgetItem?.amount)
        DescriptionField(budgetItem?.description)
        TypeSelector(budgetItem?.type)
        DateField(budgetItem?.date)
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(date: String?) {
    var dateState by remember {
        mutableStateOf(date ?: EMPTY)
    }

    val calendarState = rememberSheetState()

    CalendarDialog(
        state = calendarState,
        config = CalendarConfig(
            monthSelection = true,
            yearSelection = true
        ),
        selection = CalendarSelection.Date {
            dateState = it.toString()
        }
    )

    ExposedDropdownMenuBox(
        expanded = false,
        onExpandedChange = {
            if (it) calendarState.show()
        }
    ){
        OutlinedTextField(
            modifier = Modifier.menuAnchor(),
            value = dateState,
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
private fun TypeSelector(type: BudgetItem.Type?) {
    var selectedType by remember {
        mutableStateOf(type?.value ?: EMPTY)
    }
    OutlinedDropdown(
        value = selectedType,
        label = "Tipo",
        dropdownOptions = budgetItemTypeList,
        onSelectOption = { selectedType = budgetItemTypeList[it] }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountField(amount: Double?) {
    var amountState by remember {
        mutableStateOf(amount)
    }
    OutlinedTextField(
        value = amountState?.toString() ?: "",
        onValueChange = {
            amountState = it.toDouble()
        },
        label = {
            Text(text = "Monto")
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DescriptionField(description: String?) {
    var descriptionState by remember {
        mutableStateOf(description ?: EMPTY)
    }

    OutlinedTextField(
        value = descriptionState,
        onValueChange = {
            descriptionState = it
        },
        label = {
            Text(text = "Descripcion")
        }
    )
}