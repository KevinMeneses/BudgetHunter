package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.ui.Modal
import com.meneses.budgethunter.commons.ui.OutlinedDropdown
import com.meneses.budgethunter.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetModal(
    show: Boolean,
    budgetAmount: Double,
    onDismiss: () -> Unit,
    onSaveClick: (Double) -> Unit
) {
    if (show) {
        Modal(onDismiss = onDismiss) {
            Text(
                text = "Presupuesto",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            var budget by remember {
                val amount = if (budgetAmount == 0.0) EMPTY
                else budgetAmount.toBigDecimal().toPlainString()
                mutableStateOf(amount)
            }

            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it },
                label = { Text(text = "Monto") },
                modifier = Modifier.padding(bottom = 25.dp),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                )
            )

            Button(
                onClick = {
                    onSaveClick(budget.toDoubleOrNull() ?: 0.0)
                    onDismiss()
                },
                content = { Text(text = "Guardar") }
            )
        }
    }
}

@Composable
fun FilterModal(
    show: Boolean,
    filter: BudgetEntry.Type?,
    onDismiss: () -> Unit,
    onClean: () -> Unit,
    onApply: (BudgetEntry.Type) -> Unit
) {
    if (show) {
        Modal(onDismiss = onDismiss) {
            Text(
                text = "Filtrar",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            var budgetType by remember {
                mutableStateOf(filter)
            }

            ListTypeDropdown(
                type = budgetType,
                onSelectItem = { budgetType = it }
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
                    onClick = {
                        onClean()
                        onDismiss()
                    },
                    content = { Text(text = "Limpiar") }
                )

                Button(
                    onClick = {
                        onApply(budgetType ?: return@Button)
                        onDismiss()
                    },
                    content = { Text(text = "Aplicar") }
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    if (show) {
        Modal(onDismiss = onDismiss) {
            Text(
                text = "¿Está seguro que desea eliminar este presupuesto?",
                modifier = Modifier.padding(bottom = 20.dp),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

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
                    onClick = onDismiss,
                    content = { Text(text = "Cancelar") }
                )
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.error,
                        contentColor = AppColors.onError
                    ),
                    onClick = {
                        onAccept()
                        onDismiss()
                    },
                    content = {
                        Text(text = "Eliminar")
                    }
                )
            }
        }
    }
}

@Composable
private fun ListTypeDropdown(
    type: BudgetEntry.Type?,
    onSelectItem: (BudgetEntry.Type) -> Unit
) {
    OutlinedDropdown(
        value = type?.value ?: EMPTY,
        label = "Tipo",
        dropdownOptions = BudgetEntry.getItemTypes().map { it.value },
        onSelectOption = { onSelectItem(BudgetEntry.getItemTypes()[it]) }
    )
}