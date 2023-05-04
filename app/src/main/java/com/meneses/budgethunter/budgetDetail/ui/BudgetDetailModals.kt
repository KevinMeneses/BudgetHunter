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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.ui.AmountField
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
                text = "Presupuesto",
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
                content = { Text(text = "Guardar") }
            )
        }
    }
}

@Composable
fun FilterModal(
    show: Boolean,
    filter: BudgetEntry?,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    if (show) {
        var description by remember {
            mutableStateOf(filter?.description ?: EMPTY)
        }

        var type by remember {
            mutableStateOf(filter?.type ?: BudgetEntry.Type.BOTH)
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
                val entryFilter = filter ?: BudgetEntry()
                val updatedFilter = entryFilter.copy(
                    description = description,
                    type = type
                )
                onEvent(BudgetDetailEvent.FilterEntries(updatedFilter))
                onDismiss()
            }
        }

        Modal(onDismiss = onDismiss) {
            Text(
                text = "Filtrar",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            DescriptionField(
                description = description,
                onDescriptionChanged = { description = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            TypeSelector(
                type = type,
                onTypeSelected = { type = it }
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
                    content = { Text(text = "Limpiar") }
                )

                Button(
                    onClick = onApply,
                    content = { Text(text = "Aplicar") }
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationModal(
    show: Boolean,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    val onDismiss = remember {
        fun() { onEvent(BudgetDetailEvent.ToggleDeleteModal(false)) }
    }

    val onConfirm = remember {
        fun() { onEvent(BudgetDetailEvent.DeleteBudget) }
    }

    ConfirmationModal(
        show = show,
        message = "¿Está seguro que desea eliminar este presupuesto?",
        confirmButtonText = "Eliminar",
        cancelButtonText = "Cancelar",
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
