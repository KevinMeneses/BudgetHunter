package com.meneses.budgethunter.budgetEntry.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxkeppeker.sheets.core.models.base.rememberSheetState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.OutlinedDropdown
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.commons.util.fromCurrency
import com.meneses.budgethunter.commons.util.toCurrency
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.green_success

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetEntryForm(
        budgetEntry = BudgetEntry(type = BudgetEntry.Type.INCOME),
        amountError = null,
        paddingValues = PaddingValues(0.dp),
        onBudgetItemChanged = {},
        onInvoiceFieldClick = {}
    )
}

@Composable
fun BudgetEntryForm(
    budgetEntry: BudgetEntry,
    @StringRes amountError: Int?,
    paddingValues: PaddingValues,
    onBudgetItemChanged: (BudgetEntry) -> Unit,
    onInvoiceFieldClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(all = 20.dp)
    ) {
        TypeSwitch(
            type = budgetEntry.type,
            onTypeSelected = {
                budgetEntry
                    .copy(type = it)
                    .run(onBudgetItemChanged)
            }
        )
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
        CategorySelector(
            category = budgetEntry.category,
            onCategorySelected = {
                budgetEntry
                    .copy(category = it)
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
        InvoiceField(
            onAttachInvoice = onInvoiceFieldClick,
            invoice = budgetEntry.invoice
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun InvoiceField(
    onAttachInvoice: () -> Unit,
    invoice: String?
) {
    Card(
        colors = CardDefaults.outlinedCardColors(),
        modifier = Modifier
            .dashedBorder(
                width = 1.dp,
                color = AppColors.onSecondaryContainer,
                shape = AbsoluteRoundedCornerShape(5.dp),
                on = 10.dp,
                off = 8.dp
            )
            .width(TextFieldDefaults.MinWidth)
            .clickable(onClick = onAttachInvoice)
            .padding(15.dp)
    ) {
        val invoiceText = invoice
            ?.let { stringResource(R.string.invoice, it.split("/").last()) }
            ?: stringResource(R.string.attach_invoice)

        Text(text = invoiceText)
    }
    Spacer(modifier = Modifier.height(10.dp))
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
fun TypeSwitch(
    type: BudgetEntry.Type?,
    onTypeSelected: (BudgetEntry.Type) -> Unit
) {
    val itemTypes = remember { BudgetEntry.getItemTypes() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = itemTypes.first().toStringResource(),
            fontSize = 16.sp
        )
        Switch(
            checked = type == BudgetEntry.Type.INCOME,
            onCheckedChange = {
                val selection =
                    if (it) BudgetEntry.Type.INCOME
                    else BudgetEntry.Type.OUTCOME
                onTypeSelected(selection)
            },
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = AppColors.onError,
                uncheckedTrackColor = AppColors.error,
                uncheckedBorderColor = AppColors.error,
                checkedTrackColor = green_success
            )
        )
        Text(
            text = itemTypes.last().toStringResource(),
            fontSize = 16.sp
        )
    }
}

@Composable
fun CategorySelector(
    category: BudgetEntry.Category?,
    onCategorySelected: (BudgetEntry.Category) -> Unit
) {
    val categories = remember { BudgetEntry.getCategories() }
    OutlinedDropdown(
        value = category?.toStringResource() ?: EMPTY,
        label = stringResource(id = R.string.category),
        dropdownOptions = categories.map { it.toStringResource() },
        onSelectOption = { onCategorySelected(categories[it]) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountField(
    amount: String,
    onAmountChanged: (String) -> Unit,
    @StringRes amountError: Int? = null
) {
    OutlinedTextField(
        value = TextFieldValue(
            text = amount.toCurrency(),
            selection = TextRange(amount.toCurrency().length)
        ),
        onValueChange = {
            onAmountChanged(it.text.fromCurrency())
            // TODO: fix decimal use case
            /*if (it.isBlank()) {
                onAmountChanged(EMPTY)
                return@OutlinedTextField
            }
            val decimals = it.split(".").getOrNull(1) ?: EMPTY
            if (decimals == "00") return@OutlinedTextField
            val decimalsLength = decimals.length
            if (decimalsLength > 2) return@OutlinedTextField
            val amountNumber = it.toDoubleOrNull() ?: return@OutlinedTextField
            if (amountNumber > 0) onAmountChanged(it)*/
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
