package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.amount
import budgethunter.composeapp.generated.resources.category
import budgethunter.composeapp.generated.resources.date
import budgethunter.composeapp.generated.resources.description
import budgethunter.composeapp.generated.resources.entry_date
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.toStringResource
import com.meneses.budgethunter.commons.ui.OutlinedDropdown
import com.meneses.budgethunter.commons.ui.SimpleDatePickerDialog
import com.meneses.budgethunter.commons.ui.ThousandSeparatorTransformation
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.green_success
import org.jetbrains.compose.resources.stringResource

@Composable
fun AmountField(
    amount: String,
    onAmountChanged: (String) -> Unit,
    amountError: String? = null
) {
    OutlinedTextField(
        value = amount,
        onValueChange = { newValue ->
            val filtered = newValue.filter { it.isDigit() || it == '.' }
            val parts = filtered.split(".")
            val cleanValue = if (parts.size > 2) {
                parts[0] + "." + parts.drop(1).joinToString("")
            } else {
                filtered
            }
            val noLeadingZeros = when {
                cleanValue.isEmpty() -> ""
                cleanValue == "0" -> cleanValue
                cleanValue.startsWith("0.") -> cleanValue
                cleanValue.startsWith("0") && !cleanValue.contains(".") -> {
                    cleanValue.dropWhile { it == '0' }.ifEmpty { "0" }
                }
                else -> cleanValue
            }
            val finalValue = if (noLeadingZeros.contains(".")) {
                val decimalParts = noLeadingZeros.split(".")
                val integerPart = decimalParts[0]
                val decimalPart = decimalParts[1].take(2)
                if (decimalPart.isEmpty()) {
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
        label = { Text(text = stringResource(Res.string.amount)) },
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
                Text(text = amountError)
            }
        }
    )
}

@Composable
fun CategorySelector(
    category: BudgetEntry.Category?,
    onCategorySelected: (BudgetEntry.Category) -> Unit
) {
    val categories = remember { BudgetEntry.getCategories() }
    OutlinedDropdown(
        value = category?.toStringResource().orEmpty(),
        label = stringResource(Res.string.category),
        dropdownOptions = categories.map { it.toStringResource() },
        onSelectOption = { onCategorySelected(categories[it]) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: String?,
    label: String = stringResource(Res.string.date),
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
            value = date.orEmpty(),
            readOnly = true,
            label = { Text(text = label) },
            onValueChange = {},
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(Res.string.entry_date)
                )
            },
            singleLine = true
        )
    }
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
        label = { Text(text = stringResource(Res.string.description)) },
        maxLines = 12,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        )
    )
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
