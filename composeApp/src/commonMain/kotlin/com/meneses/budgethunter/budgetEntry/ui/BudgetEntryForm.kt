package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.category
import budgethunter.composeapp.generated.resources.date
import budgethunter.composeapp.generated.resources.save_expense
import budgethunter.composeapp.generated.resources.save_income
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource

/**
 * The entry form, ordered by how often each field changes: type and amount first, then the
 * receipt (which can fill the rest), category, description and date. Saving is the button at
 * the bottom, within reach of the thumb.
 */
@Composable
fun BudgetEntryForm(
    budgetEntry: BudgetEntry,
    amountError: String?,
    isFileValid: Boolean,
    isAiEnabled: Boolean,
    /** Opens the keyboard on the amount, for a new entry that is not starting from a receipt. */
    focusAmount: Boolean,
    paddingValues: PaddingValues,
    onBudgetItemChanged: (BudgetEntry) -> Unit,
    onInvoiceFieldClick: () -> Unit,
    onSave: () -> Unit
) {
    val amountFocus = remember { FocusRequester() }
    LaunchedEffect(focusAmount) {
        if (focusAmount) runCatching { amountFocus.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            EntryTypeSelector(
                type = budgetEntry.type,
                onTypeSelected = { budgetEntry.copy(type = it).run(onBudgetItemChanged) }
            )

            AmountHero(
                amount = budgetEntry.amount,
                onAmountChanged = { budgetEntry.copy(amount = it).run(onBudgetItemChanged) },
                amountError = amountError,
                focusRequester = amountFocus
            )

            ReceiptCard(
                invoice = budgetEntry.invoice,
                isFileValid = isFileValid,
                isAiEnabled = isAiEnabled,
                onClick = onInvoiceFieldClick
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormSectionLabel(stringResource(Res.string.category))
                CategoryChips(
                    selected = budgetEntry.category,
                    onCategorySelected = { budgetEntry.copy(category = it).run(onBudgetItemChanged) }
                )
            }

            DescriptionField(
                description = budgetEntry.description,
                onDescriptionChanged = { budgetEntry.copy(description = it).run(onBudgetItemChanged) }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormSectionLabel(stringResource(Res.string.date))
                DateShortcuts(
                    date = budgetEntry.date,
                    onDateSelected = { budgetEntry.copy(date = it).run(onBudgetItemChanged) }
                )
            }

            EntryAuthorship(entry = budgetEntry)
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.primary,
                contentColor = AppColors.onPrimary
            )
        ) {
            Text(
                text = stringResource(
                    if (budgetEntry.type == BudgetEntry.Type.INCOME) Res.string.save_income
                    else Res.string.save_expense
                ),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
