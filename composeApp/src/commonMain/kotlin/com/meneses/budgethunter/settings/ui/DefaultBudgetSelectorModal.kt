package com.meneses.budgethunter.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.cancel
import budgethunter.composeapp.generated.resources.no_budgets_available
import budgethunter.composeapp.generated.resources.ok
import budgethunter.composeapp.generated.resources.select_default_budget
import budgethunter.composeapp.generated.resources.select_default_budget_description
import com.meneses.budgethunter.budgetList.domain.Budget
import org.jetbrains.compose.resources.stringResource

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
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.select_default_budget),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            if (availableBudgets.isEmpty()) {
                Text(
                    text = stringResource(Res.string.no_budgets_available),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column {
                    Text(
                        text = stringResource(Res.string.select_default_budget_description),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(availableBudgets) { budget ->
                            BudgetOptionItem(
                                budget = budget,
                                isSelected = selectedBudget?.id == budget.id,
                                onSelect = { selectedBudget = budget }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (availableBudgets.isNotEmpty()) {
                TextButton(
                    onClick = {
                        selectedBudget?.let { onBudgetSelected(it) }
                        onDismiss()
                    },
                    enabled = selectedBudget != null
                ) {
                    Text(
                        text = stringResource(Res.string.ok),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.cancel),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun BudgetOptionItem(
    budget: Budget,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = budget.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (budget.amount > 0) {
                    Text(
                        text = "$${budget.amount}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
