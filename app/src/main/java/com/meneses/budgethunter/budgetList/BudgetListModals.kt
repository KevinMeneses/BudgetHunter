package com.meneses.budgethunter.budgetList

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.model.Budget
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.Modal
import com.meneses.budgethunter.commons.OutlinedDropdown
import com.meneses.budgethunter.theme.AppColors

@Composable
fun FilterListModal(
    show: Boolean,
    filter: Budget?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onApplyClick: (Budget) -> Unit,
) {
    if (show) {
        Modal(onDismiss = onDismiss) {
            var name by remember {
                mutableStateOf(filter?.name ?: EMPTY)
            }

            val frequencyList = remember {
                Budget.Frequency.getFrequencies()
            }

            var frequency by remember {
                mutableStateOf(filter?.frequency)
            }

            ModalContent(
                title = "Filtrar",
                name = name,
                frequency = frequency,
                frequencyOptions = frequencyList.map { it.value },
                onNameChanged = { name = it },
                onFrequencyChanged = { frequency = frequencyList[it] }
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
                        onClear()
                        onDismiss()
                    }
                ) {
                    Text(text = "Limpiar")
                }

                Button(
                    onClick = {
                        val budget = Budget(name = name, frequency = frequency)
                        onApplyClick(budget)
                        onDismiss()
                    }
                ) {
                    Text(text = "Aplicar")
                }
            }
        }
    }
}

@Composable
fun NewBudgetModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onCreateClick: (Budget) -> Unit
) {
    if (show) {
        Modal(onDismiss = onDismiss) {
            var name by remember {
                mutableStateOf(EMPTY)
            }

            val frequencyList = remember {
                Budget.Frequency.getFrequencies()
            }

            var frequency by remember {
                mutableStateOf(Budget.Frequency.UNIQUE)
            }

            ModalContent(
                title = "Nuevo presupuesto",
                name = name,
                frequency = frequency,
                frequencyOptions = frequencyList.map { it.value },
                onNameChanged = { name = it },
                onFrequencyChanged = { frequency = frequencyList[it] }
            )

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val budget = Budget(name = name, frequency = frequency)
                    onCreateClick(budget)
                    onDismiss()
                }
            ) {
                Text(text = "Crear")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalContent(
    title: String,
    name: String,
    frequency: Budget.Frequency?,
    frequencyOptions: List<String>,
    onNameChanged: (String) -> Unit,
    onFrequencyChanged: (Int) -> Unit
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
        label = { Text(text = "Nombre") }
    )

    OutlinedDropdown(
        value = frequency?.value ?: EMPTY,
        label = "Frecuencia",
        dropdownOptions = frequencyOptions,
        onSelectOption = onFrequencyChanged
    )

    Spacer(modifier = Modifier.height(30.dp))
}