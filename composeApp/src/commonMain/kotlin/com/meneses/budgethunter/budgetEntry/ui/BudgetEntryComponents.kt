package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
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
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.amount
import budgethunter.composeapp.generated.resources.date
import budgethunter.composeapp.generated.resources.description
import budgethunter.composeapp.generated.resources.entry_date
import budgethunter.composeapp.generated.resources.amount_placeholder
import com.meneses.budgethunter.commons.ui.SimpleDatePickerDialog
import com.meneses.budgethunter.commons.ui.ThousandSeparatorTransformation
import org.jetbrains.compose.resources.stringResource

@Composable
fun AmountField(
    amount: String,
    onAmountChanged: (String) -> Unit,
    amountError: String? = null
) {
    OutlinedTextField(
        value = amount,
        onValueChange = { onAmountChanged(sanitizeAmountInput(it)) },
        modifier = Modifier.width(TextFieldDefaults.MinWidth),
        label = { Text(text = stringResource(Res.string.amount)) },
        placeholder = { Text(text = stringResource(Res.string.amount_placeholder)) },
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
            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
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
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(Res.string.description)) },
        maxLines = 12,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        )
    )
}

/**
 * Keeps what can be an amount: digits and one dot, no leading zeros, at most two decimals.
 * "0012.345" -> "12.34", "1.2.3" -> "1.23".
 */
fun sanitizeAmountInput(newValue: String): String {
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
    return if (noLeadingZeros.contains(".")) {
        val decimalParts = noLeadingZeros.split(".")
        val integerPart = decimalParts[0]
        val decimalPart = decimalParts[1].take(2)
        if (decimalPart.isEmpty()) "$integerPart." else "$integerPart.$decimalPart"
    } else {
        noLeadingZeros
    }
}
