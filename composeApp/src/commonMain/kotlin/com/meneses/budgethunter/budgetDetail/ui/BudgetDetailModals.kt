package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.apply
import budgethunter.composeapp.generated.resources.budget
import budgethunter.composeapp.generated.resources.cancel
import budgethunter.composeapp.generated.resources.clean
import budgethunter.composeapp.generated.resources.delete
import budgethunter.composeapp.generated.resources.delete_budget_confirmation_message
import budgethunter.composeapp.generated.resources.delete_entries_confirmation_message
import budgethunter.composeapp.generated.resources.end_date
import budgethunter.composeapp.generated.resources.filter
import budgethunter.composeapp.generated.resources.filter_entries_criteria
import budgethunter.composeapp.generated.resources.save
import budgethunter.composeapp.generated.resources.set_budget_amount
import budgethunter.composeapp.generated.resources.start_date
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetEntry.ui.AmountField
import com.meneses.budgethunter.budgetEntry.ui.CategorySelector
import com.meneses.budgethunter.budgetEntry.ui.DateField
import com.meneses.budgethunter.budgetEntry.ui.DescriptionField
import com.meneses.budgethunter.budgetEntry.ui.TypeSwitch
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.ConfirmationModal
import org.jetbrains.compose.resources.stringResource

@Composable
fun BudgetModal(
    show: Boolean,
    budgetAmount: Double,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    if (show) {
        var budget by remember {
            val amount = if (budgetAmount == 0.0) EMPTY
            else budgetAmount.toString()
            mutableStateOf(amount)
        }

        val onDismiss = remember {
            fun() { onEvent(BudgetDetailEvent.ToggleBudgetModal(false)) }
        }

        val onSaveClick = remember {
            fun() {
                val amount = budget.toDoubleOrNull() ?: 0.0
                onEvent(BudgetDetailEvent.UpdateBudgetAmount(amount))
                onDismiss()
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(Res.string.budget),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(Res.string.set_budget_amount),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    AmountField(
                        amount = budget,
                        onAmountChanged = { budget = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onSaveClick,
                    enabled = budget.toDoubleOrNull() != null
                ) {
                    Text(
                        text = stringResource(Res.string.save),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(Res.string.cancel),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            properties = DialogProperties()
        )
    }
}

@Composable
fun FilterModal(
    show: Boolean,
    filter: BudgetEntryFilter?,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    if (show) {
        val entryFilter = filter ?: BudgetEntryFilter()

        var description by remember {
            mutableStateOf(entryFilter.description)
        }

        var type by remember {
            mutableStateOf(entryFilter.type)
        }

        var category by remember {
            mutableStateOf(entryFilter.category)
        }

        var startDate by remember {
            mutableStateOf(entryFilter.startDate)
        }

        var endDate by remember {
            mutableStateOf(entryFilter.endDate)
        }

        val onDismiss = remember {
            fun() { onEvent(BudgetDetailEvent.ToggleFilterModal(false)) }
        }

        val onClear = remember {
            fun() {
                onEvent(BudgetDetailEvent.ClearFilter)
                onDismiss()
            }
        }

        val onApply = remember {
            fun() {
                if (
                    description.isNullOrBlank() &&
                    type == null &&
                    category == null &&
                    startDate == null &&
                    endDate == null
                ) {
                    onEvent(BudgetDetailEvent.ClearFilter)
                } else {
                    val budgetEntryFilter = BudgetEntryFilter(
                        description = description,
                        type = type,
                        category = category,
                        startDate = startDate,
                        endDate = endDate
                    )
                    onEvent(BudgetDetailEvent.FilterEntries(budgetEntryFilter))
                }
                onDismiss()
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(Res.string.filter),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(Res.string.filter_entries_criteria),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    DescriptionField(
                        description = description.orEmpty(),
                        onDescriptionChanged = { description = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TypeSwitch(
                        type = type,
                        onTypeSelected = { type = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CategorySelector(
                        category = category,
                        onCategorySelected = { category = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DateField(
                        date = startDate,
                        label = stringResource(Res.string.start_date),
                        onDateSelected = { startDate = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DateField(
                        date = endDate,
                        label = stringResource(Res.string.end_date),
                        onDateSelected = { endDate = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onApply) {
                    Text(
                        text = stringResource(Res.string.apply),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onClear) {
                    Text(
                        text = stringResource(Res.string.clean),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            properties = DialogProperties()
        )
    }
}

@Composable
fun DeleteBudgetConfirmationModal(
    show: Boolean,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    val onDismiss = remember {
        fun() { onEvent(BudgetDetailEvent.ToggleDeleteBudgetModal(false)) }
    }

    val onConfirm = remember {
        fun() { onEvent(BudgetDetailEvent.DeleteBudget) }
    }

    ConfirmationModal(
        show = show,
        message = stringResource(Res.string.delete_budget_confirmation_message),
        confirmButtonText = stringResource(Res.string.delete),
        cancelButtonText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun DeleteEntriesConfirmationModal(
    show: Boolean,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    val onDismiss = remember {
        fun() { onEvent(BudgetDetailEvent.ToggleDeleteEntriesModal(false)) }
    }

    val onConfirm = remember {
        fun() { onEvent(BudgetDetailEvent.DeleteSelectedEntries) }
    }

    ConfirmationModal(
        show = show,
        message = stringResource(Res.string.delete_entries_confirmation_message),
        confirmButtonText = stringResource(Res.string.delete),
        cancelButtonText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}