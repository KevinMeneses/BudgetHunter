package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.DefDivider
import com.meneses.budgethunter.theme.AppColors

@Composable
fun BudgetDetailContent(
    paddingValues: PaddingValues,
    uiState: BudgetDetailState,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    val onBudgetClick = remember {
        fun() { onEvent(BudgetDetailEvent.ToggleBudgetModal(true)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(
                vertical = 15.dp,
                horizontal = 20.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(0.9f, true)
        ) {
            BudgetSection(
                amount = uiState.budget.amount,
                onClick = onBudgetClick
            )
            Spacer(modifier = Modifier.height(20.dp))
            ListSection(
                budgetEntries = uiState.entries,
                isSelectionActive = uiState.isSelectionActive,
                onEvent = onEvent
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (!uiState.isSelectionActive) {
                BalanceSection(
                    budgetEntries = uiState.entries,
                    budgetAmount = uiState.budget.amount
                )
            } else DeleteButton(onEvent)
        }
    }
}

@Composable
fun BudgetSection(
    amount: Double,
    onClick: () -> Unit
) {
    val budget = if (amount == 0.0) {
        "Agregar un presupuesto"
    } else {
        "Presupuesto\n" + amount.toBigDecimal().toPlainString()
    }

    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = AppColors.tertiaryContainer,
            contentColor = AppColors.onTertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = AppColors.onTertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = budget,
                modifier = Modifier.padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.onTertiaryContainer
            )
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.ListSection(
    budgetEntries: List<BudgetEntry>,
    isSelectionActive: Boolean,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    val onSelectAllItems = remember {
        fun(isActive: Boolean) {
            onEvent(BudgetDetailEvent.ToggleAllEntriesSelection(isActive))
        }
    }

    Card(
        modifier = Modifier.weight(0.8f, true),
        colors = CardDefaults.elevatedCardColors(
            containerColor = AppColors.background,
            contentColor = AppColors.onBackground
        )
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            if (isSelectionActive) stickyHeader {
                val onCloseSelection = remember {
                    fun() { onEvent(BudgetDetailEvent.ToggleSelectionState(false)) }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.background)
                        .clickable { },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = budgetEntries.all { it.isSelected },
                        onCheckedChange = onSelectAllItems
                    )
                    Text(
                        text = "${budgetEntries.count { it.isSelected }} Seleccionados",
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = EMPTY,
                        modifier = Modifier.clickable(onClick = onCloseSelection)
                    )
                }
                DefDivider()
            }
            items(budgetEntries.size) { index ->
                val budgetItem = budgetEntries[index]

                val onItemClick = {
                    onEvent(
                        if (!isSelectionActive) BudgetDetailEvent.ShowEntry(budgetItem)
                        else BudgetDetailEvent.ToggleSelectEntry(index, !budgetItem.isSelected)
                    )
                }

                val onLongClick = remember {
                    fun() { onEvent(BudgetDetailEvent.ToggleSelectionState(true)) }
                }

                val onItemChecked = fun(isChecked: Boolean) {
                    onEvent(BudgetDetailEvent.ToggleSelectEntry(index, isChecked))
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = onItemClick,
                            onLongClick = onLongClick
                        )
                        .padding(vertical = if (isSelectionActive) 0.dp else 10.dp)
                ) {
                    if (isSelectionActive) Checkbox(
                        checked = budgetItem.isSelected,
                        onCheckedChange = onItemChecked
                    )
                    Text(
                        text = budgetItem.description
                            .takeIf { it.isNotBlank() }
                            ?: "Sin descripcion",
                        modifier = Modifier.weight(0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val operatorSign =
                        if (budgetItem.type == BudgetEntry.Type.OUTCOME) "-"
                        else EMPTY
                    Text(
                        text = operatorSign + budgetItem.amount.toString(),
                        modifier = Modifier.weight(0.3f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
                if (index != budgetEntries.size - 1) DefDivider()
            }
        }
    }
}

@Composable
fun BalanceSection(
    budgetEntries: List<BudgetEntry>,
    budgetAmount: Double
) {
    val outcomes = budgetEntries
        .filter { it.type == BudgetEntry.Type.OUTCOME }
        .map { it.amount }
        .reduceOrNull { acc, actual -> acc + actual }
        ?: 0.0

    val incomes = budgetEntries
        .filter { it.type == BudgetEntry.Type.INCOME }
        .map { it.amount }
        .reduceOrNull { acc, actual -> acc + actual }
        ?: 0.0

    val balance = budgetAmount + incomes - outcomes

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = AppColors.primaryContainer,
            contentColor = AppColors.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = AppColors.onPrimaryContainer
        )
    ) {
        val operatorSign = if (outcomes == 0.0) EMPTY else "-"
        AmountText("Total gastos:", "$operatorSign${outcomes.toBigDecimal().toPlainString()}")
        DefDivider(color = AppColors.onSecondaryContainer)
        AmountText("Balance:", balance.toBigDecimal().toPlainString())
    }
}

@Composable
private fun DeleteButton(
    onEvent: (BudgetDetailEvent) -> Unit
) {
    val onClick = remember {
        fun() { onEvent(BudgetDetailEvent.DeleteSelectedEntries) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background)
            .padding(vertical = 5.dp),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.align(Alignment.Center),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.error,
                contentColor = AppColors.onError
            )
        ) {
            Text(text = "Eliminar")
        }
    }
}

@Composable
private fun AmountText(
    description: String,
    amount: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = description, color = AppColors.onSecondaryContainer)
        Text(text = amount, color = AppColors.onSecondaryContainer)
    }
}


