package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetEntry.ui.AmountField
import com.meneses.budgethunter.budgetEntry.ui.CategorySelector
import com.meneses.budgethunter.budgetEntry.ui.DateField
import com.meneses.budgethunter.budgetEntry.ui.DescriptionField
import com.meneses.budgethunter.budgetEntry.ui.TypeSwitch
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.ConfirmationModal

@Composable
fun BudgetModal(
    show: Boolean,
    budgetAmount: Double,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    if (show) {
        var budget by remember {
            val amount = if (budgetAmount == 0.0) EMPTY
            else budgetAmount.toBigDecimal().toPlainString()
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
                    text = stringResource(id = R.string.budget),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.set_budget_amount),
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
                        text = stringResource(id = R.string.save),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(id = R.string.cancel),
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
                    text = stringResource(id = R.string.filter),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.filter_entries_criteria),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    DescriptionField(
                        description = description ?: EMPTY,
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
                        label = stringResource(id = R.string.start_date),
                        onDateSelected = { startDate = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DateField(
                        date = endDate,
                        label = stringResource(id = R.string.end_date),
                        onDateSelected = { endDate = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onApply) {
                    Text(
                        text = stringResource(id = R.string.apply),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onClear) {
                    Text(
                        text = stringResource(id = R.string.clean),
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
        message = stringResource(id = R.string.delete_budget_confirmation_message),
        confirmButtonText = stringResource(id = R.string.delete),
        cancelButtonText = stringResource(id = R.string.cancel),
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
        message = stringResource(id = R.string.delete_entries_confirmation_message),
        confirmButtonText = stringResource(id = R.string.delete),
        cancelButtonText = stringResource(id = R.string.cancel),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun CollaborateModal(
    show: Boolean,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = {
                BudgetDetailEvent
                    .ToggleCollaborateModal(false)
                    .run(onEvent)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = stringResource(R.string.collaborate_icon_description),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(id = R.string.collaborate),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.collaborate_confirmation_message),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        BudgetDetailEvent
                            .StartCollaboration
                            .run(onEvent)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.collaborate),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        BudgetDetailEvent
                            .ToggleCollaborateModal(false)
                            .run(onEvent)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            properties = DialogProperties()
        )
    }
}

@Composable
fun CodeModal(
    code: Int?,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    if (code != null) {
        val hideCodeModal = {
            BudgetDetailEvent
                .HideCodeModal
                .run(onEvent)
        }

        AlertDialog(
            onDismissRequest = hideCodeModal,
            icon = {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = stringResource(R.string.collaboration_code_icon_description),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.collaboration_code),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.share_code_with_collaborators),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "$code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = hideCodeModal) {
                    Text(
                        text = stringResource(R.string.ok),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            properties = DialogProperties()
        )
    }
}
