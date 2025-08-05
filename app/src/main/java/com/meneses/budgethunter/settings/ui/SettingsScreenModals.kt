package com.meneses.budgethunter.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.domain.Budget

@Composable
fun DefaultBudgetSelectorModal(
    availableBudgets: List<Budget>,
    currentDefaultBudget: Budget?,
    onDismiss: () -> Unit,
    onBudgetSelected: (Budget) -> Unit
) {
    var selectedBudget by remember { mutableStateOf(currentDefaultBudget) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.select_default_budget)) },
        text = {
            if (availableBudgets.isEmpty()) {
                Text("No hay presupuestos disponibles para seleccionar.")
            } else {
                Column {
                    Text("Selecciona un presupuesto por defecto para las transacciones automáticas:")
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn {
                        items(availableBudgets) { budget ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedBudget = budget }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selectedBudget?.id == budget.id),
                                    onClick = { selectedBudget = budget }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = budget.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedBudget?.let { onBudgetSelected(it) }
                    onDismiss()
                },
                enabled = selectedBudget != null && availableBudgets.isNotEmpty()
            ) {
                Text(stringResource(id = R.string.save_default_budget))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}


