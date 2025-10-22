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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.ui.OutlinedDropdown
import com.meneses.budgethunter.commons.ui.SimpleDatePickerDialog
import com.meneses.budgethunter.commons.ui.ThousandSeparatorTransformation
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.commons.ui.formatDateForDisplay
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
            .padding(vertical = 15.dp, horizontal = 10.dp)
    ) {
        Row {
            val invoiceText = invoice
                ?.split("/")
                ?.last()
                ?: stringResource(R.string.attach_invoice)

            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                tint = AppColors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = invoiceText)
        }
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
    var showDatePicker by remember { mutableStateOf(false) }

    SimpleDatePickerDialog(
        showDialog = showDatePicker,
        onDateSelected = onDateSelected,
        onDismiss = { showDatePicker = false },
        initialDate = date
    )

    ExposedDropdownMenuBox(
        expanded = false,
        onExpandedChange = {
            if (it) showDatePicker = true
        }
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(),
            value = formatDateForDisplay(date),
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
        horizontalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = AppColors.surface
            ),
            shape = AbsoluteRoundedCornerShape(25.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTypeSelected(BudgetEntry.Type.OUTCOME) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (type == BudgetEntry.Type.OUTCOME) AppColors.error else AppColors.surface
                    ),
                    shape = AbsoluteRoundedCornerShape(25.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = itemTypes.first().toStringResource(),
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = if (type == BudgetEntry.Type.OUTCOME) AppColors.onError else AppColors.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTypeSelected(BudgetEntry.Type.INCOME) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (type == BudgetEntry.Type.INCOME) green_success else AppColors.surface
                    ),
                    shape = AbsoluteRoundedCornerShape(25.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = itemTypes.last().toStringResource(),
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = if (type == BudgetEntry.Type.INCOME) AppColors.surface else AppColors.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    category: BudgetEntry.Category?,
    onCategorySelected: (BudgetEntry.Category) -> Unit
) {
    val categories = remember { BudgetEntry.getCategories() }
    OutlinedDropdown(
        value = category?.toStringResource().orEmpty(),
        label = stringResource(id = R.string.category),
        dropdownOptions = categories.map { it.toStringResource() },
        onSelectOption = { onCategorySelected(categories[it]) }
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
        onValueChange = { newValue ->
            // Only allow digits and decimal point
            val filtered = newValue.filter { it.isDigit() || it == '.' }

            // Handle multiple decimal points - keep only the first one
            val parts = filtered.split(".")
            val cleanValue = if (parts.size > 2) {
                parts[0] + "." + parts.drop(1).joinToString("")
            } else {
                filtered
            }

            // Prevent leading zeros (except for "0.")
            val noLeadingZeros = when {
                cleanValue.isEmpty() -> ""
                cleanValue == "0" -> cleanValue // Allow single "0"
                cleanValue.startsWith("0.") -> cleanValue // Allow "0.xx"
                cleanValue.startsWith("0") && !cleanValue.contains(".") -> {
                    // Remove leading zeros from integer part
                    cleanValue.dropWhile { it == '0' }.ifEmpty { "0" }
                }
                else -> cleanValue
            }

            // Limit decimal places to 2
            val finalValue = if (noLeadingZeros.contains(".")) {
                val decimalParts = noLeadingZeros.split(".")
                val integerPart = decimalParts[0]
                val decimalPart = decimalParts[1].take(2)
                if (decimalPart.isEmpty()) {
                    // Keep the decimal point even if no digits follow yet
                    "$integerPart."
                } else {
                    "$integerPart.$decimalPart"
                }
            } else {
                noLeadingZeros
            }

            onAmountChanged(finalValue)
        },
        modifier = Modifier.width(TextFieldDefaults.MinWidth),
        label = { Text(text = stringResource(id = R.string.amount)) },
        placeholder = { Text(text = "0.00") },
        prefix = { Text(text = "$") },
        visualTransformation = ThousandSeparatorTransformation(),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Decimal
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
        modifier = Modifier.width(TextFieldDefaults.MinWidth),
        label = { Text(text = stringResource(id = R.string.description)) },
        maxLines = 12,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        )
    )
}
