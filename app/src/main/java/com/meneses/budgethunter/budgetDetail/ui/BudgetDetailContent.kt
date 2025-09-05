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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.CompottiePlaceholder
import com.meneses.budgethunter.commons.ui.DefDivider
import com.meneses.budgethunter.commons.ui.LoadingScreen
import com.meneses.budgethunter.commons.util.loadRawResourceAsString
import com.meneses.budgethunter.commons.util.toCurrency
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.green_success

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetDetailContent(
        paddingValues = PaddingValues(0.dp),
        uiState = BudgetDetailState()
    ) {
    }
}

@Composable
fun BudgetDetailContent(
    paddingValues: PaddingValues,
    uiState: BudgetDetailState,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    val onBudgetClick = remember {
        fun() { onEvent(BudgetDetailEvent.ToggleBudgetModal(true)) }
    }

    if (uiState.isLoading) {
        LoadingScreen()
    } else {
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
                    amount = uiState.budgetDetail.budget.amount,
                    onClick = onBudgetClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (uiState.budgetDetail.entries.isEmpty()) {
                    loadRawResourceAsString(R.raw.empty_list)?.let {
                        CompottiePlaceholder(
                            jsonContent = it,
                            modifier = Modifier
                                .padding(vertical = 40.dp)
                                .weight(0.8f, true)
                        )
                    }
                } else {
                    ListSection(
                        budgetEntries = uiState.budgetDetail.entries,
                        isSelectionActive = uiState.isSelectionActive,
                        listOrder = uiState.listOrder,
                        onEvent = onEvent
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!uiState.isSelectionActive) {
                    BalanceSection(
                        budgetEntries = uiState.budgetDetail.entries,
                        budgetAmount = uiState.budgetDetail.budget.amount
                    )
                } else {
                    DeleteButton(onEvent)
                }
            }
        }
    }
}

@Composable
fun BudgetSection(
    amount: Double,
    onClick: () -> Unit
) {
    val budget = if (amount == 0.0) {
        stringResource(id = R.string.add_budget)
    } else {
        stringResource(id = R.string.budget_amount, amount.toCurrency())
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
    listOrder: BudgetDetailState.ListOrder,
    onEvent: (BudgetDetailEvent) -> Unit
) {
    var showDate by remember {
        mutableStateOf(false)
    }

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
                        .background(AppColors.background),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = budgetEntries.all { it.isSelected },
                        onCheckedChange = onSelectAllItems
                    )
                    val selectedEntries = budgetEntries.count { it.isSelected }.toString()
                    Text(
                        text = stringResource(id = R.string.selected_entries, selectedEntries),
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        modifier = Modifier.offset(10.dp),
                        onClick = onCloseSelection
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_entries_selection_mode)
                        )
                    }
                }
                DefDivider()
            } else stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.background),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headerText =
                        if (showDate) R.string.date
                        else R.string.description

                    TextButton(
                        modifier = Modifier.offset((-10).dp),
                        onClick = {
                            showDate = !showDate
                        }
                    ) {
                        Text(
                            text = stringResource(id = headerText),
                            color = AppColors.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val orderIcon = when (listOrder) {
                        BudgetDetailState.ListOrder.DEFAULT -> Icons.AutoMirrored.Filled.List
                        BudgetDetailState.ListOrder.AMOUNT_ASCENDANT -> Icons.Default.KeyboardArrowDown
                        BudgetDetailState.ListOrder.AMOUNT_DESCENDANT -> Icons.Default.KeyboardArrowUp
                    }

                    IconButton(
                        modifier = Modifier.offset(x = 10.dp),
                        onClick = {
                            onEvent(BudgetDetailEvent.SortList)
                        }
                    ) {
                        Icon(
                            imageVector = orderIcon,
                            contentDescription = stringResource(R.string.budget_list_icon_description)
                        )
                    }
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
                            onLongClick = {
                                onLongClick()
                                onItemChecked(true)
                            }
                        )
                        .padding(vertical = if (isSelectionActive) 0.dp else 13.6.dp)
                ) {
                    if (isSelectionActive) Checkbox(
                        checked = budgetItem.isSelected,
                        onCheckedChange = onItemChecked
                    )

                    val budgetItemText =
                        if (showDate) budgetItem.date
                        else budgetItem.description

                    Text(
                        text = budgetItemText
                            .takeIf { it.isNotBlank() }
                            ?: stringResource(id = R.string.no_description),
                        modifier = Modifier.weight(0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val operatorSign: String
                    val color: Color

                    if (budgetItem.type == BudgetEntry.Type.OUTCOME) {
                        operatorSign = "-"
                        color = AppColors.error
                    } else {
                        operatorSign = "+"
                        color = green_success
                    }

                    Text(
                        text = operatorSign + budgetItem.amount.toCurrency(),
                        color = color,
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
        .map { it.amount.toDouble() }
        .reduceOrNull { acc, actual -> acc + actual }
        ?: 0.0

    val incomes = budgetEntries
        .filter { it.type == BudgetEntry.Type.INCOME }
        .map { it.amount.toDouble() }
        .reduceOrNull { acc, actual -> acc + actual }
        ?: 0.0

    val outcomesText = outcomes.toCurrency()

    val balance = budgetAmount + incomes - outcomes
    val balanceText = balance.toCurrency()

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
        AmountText(
            description = stringResource(id = R.string.total_outcomes),
            amount = "$operatorSign$outcomesText"
        )
        DefDivider(color = AppColors.onSecondaryContainer)
        AmountText(
            description = stringResource(id = R.string.balance),
            amount = balanceText
        )
    }
}

@Composable
private fun DeleteButton(
    onEvent: (BudgetDetailEvent) -> Unit
) {
    val onClick = remember {
        fun() { onEvent(BudgetDetailEvent.ToggleDeleteEntriesModal(true)) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background)
            .padding(vertical = 5.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.align(Alignment.Center),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.error,
                contentColor = AppColors.onError
            )
        ) {
            Text(text = stringResource(id = R.string.delete))
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
