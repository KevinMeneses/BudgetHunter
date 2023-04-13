package com.meneses.budgethunter.ui.insAndOuts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.model.BudgetDetail
import com.meneses.budgethunter.ui.budgetListFilterOptions
import com.meneses.budgethunter.ui.commons.EMPTY
import com.meneses.budgethunter.ui.commons.Modal
import com.meneses.budgethunter.ui.commons.OutlinedDropdown
import com.meneses.budgethunter.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onSaveClick: (String) -> Unit
) {
    if (show) {
        Modal(onDismiss = onDismiss) {
            Text(
                text = "Presupuesto",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            var amount by remember { mutableStateOf(EMPTY) }
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(text = "Monto") },
                modifier = Modifier.padding(bottom = 25.dp)
            )

            Button(
                onClick = { onSaveClick(amount) },
                content = { Text(text = "Guardar") }
            )
        }
    }
}

@Composable
fun FilterModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onClean: () -> Unit,
    onApply: () -> Unit
) {
    if (show) {
        Modal(onDismiss = onDismiss) {
            Text(
                text = "Filtrar",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            ListTypeDropdown()
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
                    onClick = onClean,
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
                    onClick = onAccept,
                    content = {
                        Text(text = "Eliminar")
                    }
                )
            }
        }
    }
}

@Composable
private fun ListTypeDropdown() {
    var dropdownSelectedItem by remember {
        mutableStateOf(BudgetDetail.Type.OUTCOME.value)
    }

    OutlinedDropdown(
        value = dropdownSelectedItem,
        label = "Tipo",
        dropdownOptions = budgetListFilterOptions,
        onSelectOption = { dropdownSelectedItem = budgetListFilterOptions[it] }
    )
}