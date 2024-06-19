package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetEntry.ui.AmountField
import com.meneses.budgethunter.budgetEntry.ui.DateField
import com.meneses.budgethunter.budgetEntry.ui.DescriptionField
import com.meneses.budgethunter.budgetEntry.ui.TypeSelector
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.ConfirmationModal
import com.meneses.budgethunter.commons.ui.Modal
import com.meneses.budgethunter.theme.AppColors

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

        Modal(onDismiss = onDismiss) {
            Text(
                text = stringResource(id = R.string.budget),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            AmountField(
                amount = budget,
                onAmountChanged = { budget = it }
            )

            Spacer(modifier = Modifier.height(25.dp))

            Button(
                onClick = onSaveClick,
                content = { Text(text = stringResource(id = R.string.save)) }
            )
        }
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
                    startDate.isNullOrBlank() &&
                    endDate.isNullOrBlank()
                ) return

                val updatedFilter = entryFilter.copy(
                    description = description,
                    type = type,
                    startDate = startDate,
                    endDate = endDate
                )
                onEvent(BudgetDetailEvent.FilterEntries(updatedFilter))
                onDismiss()
            }
        }

        Modal(onDismiss = onDismiss) {
            Text(
                text = stringResource(id = R.string.filter),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            DescriptionField(
                description = description ?: EMPTY,
                onDescriptionChanged = { description = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            TypeSelector(
                type = type,
                onTypeSelected = { type = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            DateField(
                date = startDate,
                label = stringResource(id = R.string.start_date),
                onDateSelected = { startDate = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            DateField(
                date = endDate,
                label = stringResource(id = R.string.end_date),
                onDateSelected = { endDate = it }
            )

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    border = BorderStroke(width = 1.dp, color = AppColors.onBackground),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.background,
                        contentColor = AppColors.onBackground
                    ),
                    onClick = onClear,
                    content = { Text(text = stringResource(id = R.string.clean)) }
                )

                Button(
                    onClick = onApply,
                    content = { Text(text = stringResource(id = R.string.apply)) }
                )
            }
        }
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
    ConfirmationModal(
        show = show,
        message = stringResource(id = R.string.collaborate_confirmation_message),
        confirmButtonText = stringResource(id = R.string.collaborate),
        cancelButtonText = stringResource(id = R.string.cancel),
        onDismiss = {
            BudgetDetailEvent
                .ToggleCollaborateModal(false)
                .run(onEvent)
        },
        onConfirm = {
            BudgetDetailEvent
                .StartCollaboration
                .run(onEvent)
        }
    )
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

        Modal(onDismiss = hideCodeModal) {
            Text(text = "Share this code with your collaborators")
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "$code",
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.height(35.dp))
            Button(
                onClick = hideCodeModal,
                content = { Text(text = "Ok") }
            )
        }
    }
}
