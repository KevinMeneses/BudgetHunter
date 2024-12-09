package com.meneses.budgethunter.budgetList.ui

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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.Modal
import com.meneses.budgethunter.theme.AppColors

@Composable
fun FilterListModal(
    show: Boolean,
    filter: BudgetFilter?,
    onEvent: (BudgetListEvent) -> Unit
) {
    if (show) {
        val onDismiss = remember {
            fun() {
                BudgetListEvent
                    .ToggleFilterModal(false)
                    .run(onEvent)
            }
        }

        Modal(onDismiss = onDismiss) {
            var name by remember {
                mutableStateOf(filter?.name ?: EMPTY)
            }

            ModalContent(
                title = stringResource(id = R.string.filter),
                name = name,
                onNameChanged = { name = it }
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
                    onClick = {
                        BudgetListEvent.ClearFilter.run(onEvent)
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(id = R.string.clean))
                }

                Button(
                    onClick = {
                        val budgetFilter = BudgetFilter(name)
                        BudgetListEvent.FilterList(budgetFilter).run(onEvent)
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(id = R.string.apply))
                }
            }
        }
    }
}

@Composable
fun NewBudgetModal(
    show: Boolean,
    onEvent: (BudgetListEvent) -> Unit
) {
    if (show) {
        val onDismiss = remember {
            fun() {
                BudgetListEvent
                    .ToggleAddModal(false)
                    .run(onEvent)
            }
        }

        Modal(onDismiss = onDismiss) {
            var name by remember {
                mutableStateOf(EMPTY)
            }

            ModalContent(
                title = stringResource(id = R.string.new_budget),
                name = name,
                onNameChanged = { name = it }
            )

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val budget = Budget(name = name)
                    BudgetListEvent.CreateBudget(budget).run(onEvent)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.create))
            }
        }
    }
}

@Composable
fun UpdateBudgetModal(
    budget: Budget?,
    onEvent: (BudgetListEvent) -> Unit
) {
    if (budget != null) {
        val onDismiss = remember {
            fun() {
                BudgetListEvent
                    .ToggleUpdateModal(null)
                    .run(onEvent)
            }
        }

        Modal(onDismiss = onDismiss) {
            var name by remember {
                mutableStateOf(budget.name)
            }

            ModalContent(
                title = "Actualizar presupuesto",
                name = name,
                onNameChanged = { name = it },
            )

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val updatedBudget = budget.copy(name = name)
                    BudgetListEvent.UpdateBudget(updatedBudget).run(onEvent)
                    onDismiss()
                }
            ) {
                Text(text = "Actualizar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalContent(
    title: String,
    name: String,
    onNameChanged: (String) -> Unit
) {
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 20.dp)
    )

    OutlinedTextField(
        value = name,
        modifier = Modifier.padding(bottom = 30.dp),
        onValueChange = onNameChanged,
        label = { Text(text = stringResource(id = R.string.name)) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
        Modal(
            onDismiss = onDismiss
        ) {
            Text(text = "Enter the collaboration code")
            Spacer(modifier = Modifier.height(40.dp))
            TextField(
                value = collaborationCode,
                onValueChange = { collaborationCode = it }
            )
            Spacer(modifier = Modifier.height(35.dp))
            Button(
                onClick = {
                    onDismiss()
                    keyboardController?.hide()
                    BudgetListEvent
                        .JoinCollaboration(collaborationCode)
                        .run(onEvent)
                },
                content = { Text(text = "Send") }
            )
        }
    }

}
