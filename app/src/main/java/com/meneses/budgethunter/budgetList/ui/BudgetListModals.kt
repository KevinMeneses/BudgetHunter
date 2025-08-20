package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.EMPTY

@Composable
fun NewBudgetModal(
    show: Boolean,
    onEvent: (BudgetListEvent) -> Unit
) {
    if (show) {
        var name by remember {
            mutableStateOf(EMPTY)
        }

        val onDismiss = remember {
            fun() {
                BudgetListEvent
                    .ToggleAddModal(false)
                    .run(onEvent)
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(id = R.string.new_budget),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.enter_budget_name),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(text = stringResource(id = R.string.name)) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isBlank()) return@TextButton
                        val budget = Budget(name = name)
                        BudgetListEvent.CreateBudget(budget).run(onEvent)
                        onDismiss()
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(
                        text = stringResource(id = R.string.create),
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
            }
        )
    }
}

@Composable
fun UpdateBudgetModal(
    budget: Budget?,
    onEvent: (BudgetListEvent) -> Unit
) {
    if (budget != null) {
        var name by remember {
            mutableStateOf(budget.name)
        }

        val onDismiss = remember {
            fun() {
                BudgetListEvent
                    .ToggleUpdateModal(null)
                    .run(onEvent)
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.update_budget_icon_description),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.update_budget_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.modify_budget_name),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(text = stringResource(id = R.string.name)) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isBlank()) return@TextButton
                        val updatedBudget = budget.copy(name = name)
                        BudgetListEvent.UpdateBudget(updatedBudget).run(onEvent)
                        onDismiss()
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(
                        text = stringResource(R.string.update),
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
fun JoinCollaborationModal(
    show: Boolean,
    onEvent: (BudgetListEvent) -> Unit
) {
    if (show) {
        val keyboardController = LocalSoftwareKeyboardController.current
        var collaborationCode by remember { mutableStateOf("") }
        val onDismiss = {
            BudgetListEvent
                .ToggleJoinCollaborationModal(false)
                .run(onEvent)
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.join_collaboration_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.enter_collaboration_code),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TextField(
                        value = collaborationCode,
                        onValueChange = { collaborationCode = it },
                        label = { Text(text = stringResource(R.string.code)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        keyboardController?.hide()
                        BudgetListEvent
                            .JoinCollaboration(collaborationCode)
                            .run(onEvent)
                    },
                    enabled = collaborationCode.isNotBlank()
                ) {
                    Text(
                        text = stringResource(R.string.send),
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
