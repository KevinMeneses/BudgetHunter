package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.add_budget
import budgethunter.composeapp.generated.resources.balance
import budgethunter.composeapp.generated.resources.budget_amount
import budgethunter.composeapp.generated.resources.close_entries_selection_mode
import budgethunter.composeapp.generated.resources.date
import budgethunter.composeapp.generated.resources.delete
import budgethunter.composeapp.generated.resources.description
import budgethunter.composeapp.generated.resources.no_description
import budgethunter.composeapp.generated.resources.hide_filters
import budgethunter.composeapp.generated.resources.no_matching_entries
import budgethunter.composeapp.generated.resources.show_filters
import budgethunter.composeapp.generated.resources.sort_amount_ascending
import budgethunter.composeapp.generated.resources.sort_amount_descending
import budgethunter.composeapp.generated.resources.sort_entries
import budgethunter.composeapp.generated.resources.sort_newest
import budgethunter.composeapp.generated.resources.selected_entries
import budgethunter.composeapp.generated.resources.total_outcomes
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailIntent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.CompottiePlaceholder
import com.meneses.budgethunter.commons.ui.DefDivider
import com.meneses.budgethunter.commons.ui.LoadingScreen
import com.meneses.budgethunter.commons.ui.OfflineBanner
import com.meneses.budgethunter.commons.ui.SyncStatusIndicator
import com.meneses.budgethunter.commons.util.toCurrency
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.green_success
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BudgetDetailContent(
    paddingValues: PaddingValues,
    isOnline: Boolean,
    uiState: BudgetDetailState,
    onIntent: (BudgetDetailIntent) -> Unit
) {
    val onBudgetClick = remember {
        fun() { onIntent(BudgetDetailIntent.ToggleBudgetModal(true)) }
    }

    if (uiState.isLoading) {
        LoadingScreen()
    } else {
        Column(modifier = Modifier.padding(paddingValues)) {
            OfflineBanner(isOffline = !isOnline)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        vertical = 15.dp,
                        horizontal = 20.dp
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
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

                        Spacer(modifier = Modifier.height(if (uiState.isFilterBarVisible) 12.dp else 20.dp))

                        // Hidden while selecting: changing the filter would change what is selected
                        AnimatedVisibility(visible = uiState.isFilterBarVisible && !uiState.isSelectionActive) {
                            EntryFilterBar(
                                filter = uiState.filter,
                                shownCount = uiState.budgetDetail.entries.size,
                                totalCount = uiState.totalEntryCount,
                                onFilterChange = { onIntent(BudgetDetailIntent.FilterEntries(it)) },
                                onClear = { onIntent(BudgetDetailIntent.ClearFilter) },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        ListSection(
                            budgetEntries = uiState.budgetDetail.entries,
                            isFiltering = uiState.filter.isActive,
                            isFilterBarVisible = uiState.isFilterBarVisible,
                            isSelectionActive = uiState.isSelectionActive,
                            listOrder = uiState.listOrder,
                            isSyncing = uiState.isSyncingEntries,
                            onRefresh = { BudgetDetailIntent.SyncEntries.run(onIntent) },
                            onIntent = onIntent
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (!uiState.isSelectionActive) {
                            BalanceSection(
                                budgetEntries = uiState.budgetDetail.entries,
                                budgetAmount = uiState.budgetDetail.budget.amount
                            )
                        } else {
                            DeleteButton(onIntent)
                        }
                    }
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
        stringResource(Res.string.add_budget)
    } else {
        stringResource(Res.string.budget_amount, amount.toCurrency())
    }

    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = AbsoluteRoundedCornerShape(10.dp),
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.ListSection(
    budgetEntries: List<BudgetEntry>,
    isFiltering: Boolean,
    isFilterBarVisible: Boolean,
    isSelectionActive: Boolean,
    listOrder: BudgetDetailState.ListOrder,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    onIntent: (BudgetDetailIntent) -> Unit
) {
    var showDate by remember {
        mutableStateOf(false)
    }

    val onSelectAllItems = remember {
        fun(isActive: Boolean) {
            onIntent(BudgetDetailIntent.ToggleAllEntriesSelection(isActive))
        }
    }

    PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = onRefresh,
        modifier = Modifier
            .weight(0.8f, true)
            .fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = AppColors.background,
                contentColor = AppColors.onBackground
            )
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                // Kept while a filter hides every entry: it holds the button that shows the filter
                if (budgetEntries.isNotEmpty() || isFiltering) {
                    if (isSelectionActive) stickyHeader {
                        val onCloseSelection = remember {
                            fun() { onIntent(BudgetDetailIntent.ToggleSelectionState(false)) }
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
                                text = stringResource(Res.string.selected_entries, selectedEntries),
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                modifier = Modifier.offset(10.dp),
                                onClick = onCloseSelection
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.close_entries_selection_mode)
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
                            val headerText: StringResource =
                                if (showDate) Res.string.date
                                else Res.string.description

                            TextButton(
                                modifier = Modifier.offset((-10).dp),
                                onClick = {
                                    showDate = !showDate
                                }
                            ) {
                                Text(
                                    text = stringResource(headerText),
                                    color = AppColors.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Every icon takes a button's width so they are evenly spaced; the row is
                            // shifted so the last icon lines up with the amounts below
                            Row(
                                modifier = Modifier.offset(x = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SyncStatusIndicator(
                                        isSynced = budgetEntries.all { it.isSynced },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                FilterToggleButton(
                                    isFilterBarVisible = isFilterBarVisible,
                                    isFiltering = isFiltering,
                                    onClick = { onIntent(BudgetDetailIntent.ToggleFilterBar) }
                                )

                                SortButton(
                                    listOrder = listOrder,
                                    onOrderSelected = { onIntent(BudgetDetailIntent.SetListOrder(it)) }
                                )
                            }
                        }
                        DefDivider()
                    }
                }

                if (budgetEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFiltering) {
                                Text(
                                    text = stringResource(Res.string.no_matching_entries),
                                    color = AppColors.onSecondaryContainer,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                CompottiePlaceholder(
                                    fileName = "empty_list.json",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    items(budgetEntries.size) { index ->
                        val budgetItem = budgetEntries[index]

                        val onItemClick = {
                            onIntent(
                                if (!isSelectionActive) BudgetDetailIntent.ShowEntry(budgetItem)
                                else BudgetDetailIntent.ToggleSelectEntry(index, !budgetItem.isSelected)
                            )
                        }

                        val onLongClick = remember {
                            fun() { onIntent(BudgetDetailIntent.ToggleSelectionState(true)) }
                        }

                        val onItemChecked = fun(isChecked: Boolean) {
                            onIntent(BudgetDetailIntent.ToggleSelectEntry(index, isChecked))
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
                                .padding(vertical = if (isSelectionActive) 8.dp else 13.6.dp)
                        ) {
                            if (isSelectionActive) {
                                Checkbox(
                                    checked = budgetItem.isSelected,
                                    onCheckedChange = onItemChecked
                                )
                            }

                            val primaryText = if (showDate) {
                                budgetItem.date
                            } else {
                                val description = budgetItem.description
                                if (description.isNotBlank()) description else stringResource(Res.string.no_description)
                            }
                            val secondaryText = if (showDate) {
                                budgetItem.description.takeIf { it.isNotBlank() }
                            } else {
                                budgetItem.date
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                secondaryText?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!budgetItem.isSynced) {
                                    Text(
                                        text = "Pending sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.error
                                    )
                                }
                                budgetItem.createdByEmail?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = "Created by $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                budgetItem.updatedByEmail?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = "Updated by $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

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
                                modifier = Modifier.widthIn(min = 72.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                        }
                        if (index != budgetEntries.size - 1) {
                            DefDivider()
                        }
                    }
                }
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
        shape = AbsoluteRoundedCornerShape(10.dp),
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
            description = stringResource(Res.string.total_outcomes),
            amount = "$operatorSign$outcomesText"
        )
        DefDivider(color = AppColors.onSecondaryContainer)
        AmountText(
            description = stringResource(Res.string.balance),
            amount = balanceText
        )
    }
}

@Composable
private fun DeleteButton(
    onIntent: (BudgetDetailIntent) -> Unit
) {
    val onClick = remember {
        fun() { onIntent(BudgetDetailIntent.ToggleDeleteEntriesModal(true)) }
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
            Text(text = stringResource(Res.string.delete))
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

/**
 * Shows or hides the search and filter bar. Hiding it keeps the filter; a dot on the icon then
 * tells that the list is filtered.
 */
@Composable
private fun FilterToggleButton(
    isFilterBarVisible: Boolean,
    isFiltering: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (isFiltering && !isFilterBarVisible) Badge(containerColor = AppColors.primary)
            }
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(
                    if (isFilterBarVisible) Res.string.hide_filters else Res.string.show_filters
                ),
                tint = if (isFiltering || isFilterBarVisible) AppColors.primary else LocalContentColor.current
            )
        }
    }
}

/**
 * Opens a menu to choose the order of the list. Tinted while the order is not the default one,
 * which would otherwise go unnoticed.
 */
@Composable
private fun SortButton(
    listOrder: BudgetDetailState.ListOrder,
    onOrderSelected: (BudgetDetailState.ListOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(Res.string.sort_entries),
                tint = if (listOrder != BudgetDetailState.ListOrder.DEFAULT) AppColors.primary else LocalContentColor.current
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = AbsoluteRoundedCornerShape(10.dp),
            containerColor = AppColors.surface
        ) {
            listOf(
                BudgetDetailState.ListOrder.DEFAULT to Res.string.sort_newest,
                BudgetDetailState.ListOrder.AMOUNT_DESCENDANT to Res.string.sort_amount_descending,
                BudgetDetailState.ListOrder.AMOUNT_ASCENDANT to Res.string.sort_amount_ascending
            ).forEach { (order, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label), style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onOrderSelected(order)
                        expanded = false
                    },
                    trailingIcon = if (order == listOrder) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = AppColors.primary) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}
