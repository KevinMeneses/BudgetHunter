package com.meneses.budgethunter.budgetEntry.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.DateField
import com.meneses.budgethunter.commons.ui.OutlinedDropdown

@Composable
fun BudgetEntryForm(
    budgetEntry: BudgetEntry,
    @StringRes amountError: Int?,
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
                budgetEntry
                    .copy(amount = it)
                    .run(onBudgetItemChanged)
            },
            amountError = amountError
        )
        DescriptionField(
            description = budgetEntry.description,
            onDescriptionChanged = {
                budgetEntry
                    .copy(description = it)
                    .run(onBudgetItemChanged)
            }
        )
        TypeSelector(
            type = budgetEntry.type,
            onTypeSelected = {
                budgetEntry
                    .copy(type = it)
                    .run(onBudgetItemChanged)
            }
        )
        DateField(
            date = budgetEntry.date,
            onDateSelected = {
                budgetEntry
                    .copy(date = it)
                    .run(onBudgetItemChanged)
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
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

@Composable
fun AmountField(
    amount: String,
    onAmountChanged: (String) -> Unit,
    @StringRes amountError: Int? = null
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
            keyboardType = KeyboardType.Number,
            autoCorrect = false
        ),
        singleLine = true,
        isError = amountError != null,
        supportingText = {
            if (amountError != null) {
                Text(text = stringResource(id = amountError))
            }
        }
    )
}

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
