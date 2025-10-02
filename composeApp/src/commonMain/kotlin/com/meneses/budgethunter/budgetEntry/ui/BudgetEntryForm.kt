package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.attach_invoice
import budgethunter.composeapp.generated.resources.file_missing
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun BudgetEntryForm(
    budgetEntry: BudgetEntry,
    amountError: String?,
    isFileValid: Boolean,
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
            invoice = budgetEntry.invoice,
            isFileValid = isFileValid
        )
    }
}

@Composable
private fun InvoiceField(
    onAttachInvoice: () -> Unit,
    invoice: String?,
    isFileValid: Boolean
) {
    Card(
        colors = CardDefaults.outlinedCardColors(),
        modifier = Modifier
            .dashedBorder(
                width = 1.dp,
                color = if (invoice != null && !isFileValid) {
                    MaterialTheme.colorScheme.error
                } else {
                    AppColors.onSecondaryContainer
                },
                shape = AbsoluteRoundedCornerShape(5.dp),
                on = 10.dp,
                off = 8.dp
            )
            .width(TextFieldDefaults.MinWidth)
            .clickable(onClick = onAttachInvoice)
            .padding(vertical = 15.dp, horizontal = 10.dp)
    ) {
        Row {
            val invoiceText = when {
                invoice == null -> stringResource(Res.string.attach_invoice)
                !isFileValid -> stringResource(Res.string.file_missing, invoice.split("/").last())
                else -> invoice.split("/").last()
            }

            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                tint = if (invoice != null && !isFileValid) {
                    MaterialTheme.colorScheme.error
                } else {
                    AppColors.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = invoiceText,
                color = if (invoice != null && !isFileValid) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.Unspecified
                }
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}
