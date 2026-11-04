package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.add_budget_short
import budgethunter.composeapp.generated.resources.balance_label
import budgethunter.composeapp.generated.resources.budget_label
import budgethunter.composeapp.generated.resources.close_entries_selection_mode
import budgethunter.composeapp.generated.resources.delete
import budgethunter.composeapp.generated.resources.edit_budget
import budgethunter.composeapp.generated.resources.manage_collaborators
import budgethunter.composeapp.generated.resources.metrics
import budgethunter.composeapp.generated.resources.metrics_expenses
import budgethunter.composeapp.generated.resources.metrics_incomes
import budgethunter.composeapp.generated.resources.no_description
import budgethunter.composeapp.generated.resources.no_matching_entries
import budgethunter.composeapp.generated.resources.not_synced
import budgethunter.composeapp.generated.resources.search
import budgethunter.composeapp.generated.resources.selected_entries
import budgethunter.composeapp.generated.resources.sort_amount_ascending
import budgethunter.composeapp.generated.resources.sort_amount_descending
import budgethunter.composeapp.generated.resources.sort_entries
import budgethunter.composeapp.generated.resources.sort_newest
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailIntent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetDetail.domain.EntryDay
import com.meneses.budgethunter.budgetDetail.domain.groupedByDay
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.ui.CategoryBadge
import com.meneses.budgethunter.budgetEntry.ui.toStringResource
import com.meneses.budgethunter.commons.ui.CompottiePlaceholder
import com.meneses.budgethunter.commons.ui.DefDivider
import com.meneses.budgethunter.commons.ui.LoadingScreen
import com.meneses.budgethunter.commons.ui.OfflineBanner
import com.meneses.budgethunter.commons.ui.SpendingProgressBar
import com.meneses.budgethunter.commons.ui.dayHeaderLabel
import com.meneses.budgethunter.commons.ui.shortDateLabel
import com.meneses.budgethunter.commons.util.toCurrency
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.green_success
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailContent(
    paddingValues: PaddingValues,
    isOnline: Boolean,
    uiState: BudgetDetailState,
    onIntent: (BudgetDetailIntent) -> Unit,
    onMetricsClick: () -> Unit,
    /** Null while the budget is not on the server: there is nobody to share it with yet. */
    onCollaboratorsClick: (() -> Unit)?
) {
    if (uiState.isLoading) {
        LoadingScreen()
        return
    }

    Column(modifier = Modifier.padding(paddingValues)) {
        OfflineBanner(isOffline = !isOnline)

        PullToRefreshBox(
            isRefreshing = uiState.isSyncingEntries,
            onRefresh = { BudgetDetailIntent.SyncEntries.run(onIntent) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            EntriesList(
                uiState = uiState,
                onIntent = onIntent,
                onMetricsClick = onMetricsClick,
                onCollaboratorsClick = onCollaboratorsClick
            )
        }

        if (uiState.isSelectionActive) {
            DeleteButton(onIntent)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntriesList(
    uiState: BudgetDetailState,
    onIntent: (BudgetDetailIntent) -> Unit,
    onMetricsClick: () -> Unit,
    onCollaboratorsClick: (() -> Unit)?
) {
    val entries = uiState.budgetDetail.entries
    val isFiltering = uiState.filter.isActive
    // Authors are only worth a word when more than one person writes in the budget
    val showAuthors = entries.mapNotNull { it.createdByEmail?.takeIf(String::isNotBlank) }.distinct().size > 1
    // Days only make sense while the list is in date order. Computed once per list, not on every
    // recomposition of the screen.
    val days = remember(entries, uiState.listOrder) {
        if (uiState.listOrder == BudgetDetailState.ListOrder.DEFAULT) entries.groupedByDay() else null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Bottom room so the last entry can scroll above the floating buttons
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
    ) {
        item(key = "summary") {
            BudgetSummaryCard(
                incomes = uiState.budgetIncomes,
                outcomes = uiState.budgetExpenses,
                budgetAmount = uiState.budgetDetail.budget.amount,
                onBudgetClick = { onIntent(BudgetDetailIntent.ToggleBudgetModal(true)) }
            )
        }

        item(key = "actions") {
            // Hidden while selecting: changing the filter would change what is selected
            if (!uiState.isSelectionActive) {
                ActionChips(
                    uiState = uiState,
                    hasPendingSync = uiState.isAuthenticated && entries.any { !it.isSynced },
                    onIntent = onIntent,
                    onMetricsClick = onMetricsClick,
                    onCollaboratorsClick = onCollaboratorsClick
                )
            }
        }

        item(key = "filter") {
            AnimatedVisibility(visible = uiState.isFilterBarVisible && !uiState.isSelectionActive) {
                EntryFilterBar(
                    filter = uiState.filter,
                    shownCount = entries.size,
                    totalCount = uiState.totalEntryCount,
                    shownNet = entries.sumOf(BudgetEntry.Type.INCOME) - entries.sumOf(BudgetEntry.Type.OUTCOME),
                    onFilterChange = { onIntent(BudgetDetailIntent.FilterEntries(it)) },
                    onClear = { onIntent(BudgetDetailIntent.ClearFilter) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (uiState.isSelectionActive && entries.isNotEmpty()) {
            stickyHeader(key = "selection") {
                SelectionHeader(entries = entries, onIntent = onIntent)
            }
        }

        if (entries.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFiltering) {
                        Text(
                            text = stringResource(Res.string.no_matching_entries),
                            color = AppColors.onSurfaceVariant,
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
            entryItems(
                entries = entries,
                days = days,
                showAuthors = showAuthors,
                showSyncState = uiState.isAuthenticated,
                isSelectionActive = uiState.isSelectionActive,
                onIntent = onIntent
            )
        }
    }
}

/**
 * The entries, under a header with each day's net when [days] is given, or as a flat list with the
 * date on every row when it is not (amount order). Indexes passed to the intents are positions in
 * [entries], as the ViewModel expects.
 */
private fun LazyListScope.entryItems(
    entries: List<BudgetEntry>,
    days: List<EntryDay>?,
    showAuthors: Boolean,
    showSyncState: Boolean,
    isSelectionActive: Boolean,
    onIntent: (BudgetDetailIntent) -> Unit
) {
    val entryItem = { index: Int, entry: BudgetEntry ->
        item(key = "entry-${entry.id}-$index") {
            EntryRow(
                entry = entry,
                index = index,
                showDate = days == null,
                showAuthor = showAuthors,
                showSyncState = showSyncState,
                isSelectionActive = isSelectionActive,
                onIntent = onIntent
            )
        }
    }

    if (days == null) {
        entries.forEachIndexed(entryItem)
        return
    }
    days.forEach { day ->
        item(key = "day-${day.date}-${day.entries.first().index}") {
            DayHeader(date = day.date, net = day.net)
        }
        day.entries.forEach { (index, entry) -> entryItem(index, entry) }
    }
}

/**
 * Balance first, then what makes it up: the budget (tap to edit it), the income added on top and
 * the expenses. The bar is the share of the available money already spent. Always the whole
 * budget: a filter narrows the list, and its own total shows in the filter bar.
 */
@Composable
private fun BudgetSummaryCard(
    incomes: Double,
    outcomes: Double,
    budgetAmount: Double,
    onBudgetClick: () -> Unit
) {
    val available = budgetAmount + incomes
    val balance = available - outcomes
    val isOver = balance < 0
    val spentFraction = when {
        available > 0 -> (outcomes / available).toFloat()
        outcomes > 0 -> 1f
        else -> 0f
    }

    val content = AppColors.onPrimaryContainer
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = AppColors.primaryContainer,
            contentColor = content
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.balance_label),
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.8f)
            )
            Text(
                text = balance.toCurrency(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOver) AppColors.error else content,
                maxLines = 1
            )

            SpendingProgressBar(
                fraction = spentFraction.coerceIn(0f, 1f),
                isOver = isOver,
                trackColor = content.copy(alpha = 0.18f),
                fillColor = if (isOver) AppColors.error else content,
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryStat(
                    label = stringResource(Res.string.budget_label),
                    value = if (budgetAmount == 0.0) {
                        stringResource(Res.string.add_budget_short)
                    } else {
                        budgetAmount.toCurrency()
                    },
                    onClick = onBudgetClick,
                    onClickLabel = stringResource(Res.string.edit_budget)
                )
                SummaryStat(
                    label = stringResource(Res.string.metrics_incomes),
                    value = incomes.toCurrency()
                )
                SummaryStat(
                    label = stringResource(Res.string.metrics_expenses),
                    value = outcomes.toCurrency()
                )
            }
        }
    }
}

@Composable
private fun RowScope.SummaryStat(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.small)
            .then(
                if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick)
                else Modifier
            )
            .padding(vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = AppColors.onPrimaryContainer.copy(alpha = 0.8f)
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = AppColors.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(12.dp)
                )
            }
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Shortcuts that used to hide in the overflow menu, plus the search and sort controls. */
@Composable
private fun ActionChips(
    uiState: BudgetDetailState,
    hasPendingSync: Boolean,
    onIntent: (BudgetDetailIntent) -> Unit,
    onMetricsClick: () -> Unit,
    onCollaboratorsClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionChip(
            label = stringResource(Res.string.metrics),
            icon = Icons.Default.PieChart,
            onClick = onMetricsClick
        )

        if (onCollaboratorsClick != null) {
            ActionChip(
                label = stringResource(Res.string.manage_collaborators),
                icon = Icons.Default.Group,
                onClick = onCollaboratorsClick
            )
        }

        // Selected while the bar is open or a filter applies, so a hidden filter is not forgotten
        FilterChip(
            selected = uiState.isFilterBarVisible || uiState.filter.isActive,
            onClick = { onIntent(BudgetDetailIntent.ToggleFilterBar) },
            label = { Text(stringResource(Res.string.search), fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            },
            shape = MaterialTheme.shapes.small
        )

        SortChip(
            listOrder = uiState.listOrder,
            onOrderSelected = { onIntent(BudgetDetailIntent.SetListOrder(it)) }
        )

        if (hasPendingSync) {
            ActionChip(
                label = stringResource(Res.string.not_synced),
                icon = Icons.Default.CloudQueue,
                onClick = { BudgetDetailIntent.SyncEntries.run(onIntent) }
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        shape = MaterialTheme.shapes.small
    )
}

/**
 * Shows the current order and opens a menu to change it. Selected while the order is not the
 * default one, which would otherwise go unnoticed.
 */
@Composable
private fun SortChip(
    listOrder: BudgetDetailState.ListOrder,
    onOrderSelected: (BudgetDetailState.ListOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        BudgetDetailState.ListOrder.DEFAULT to Res.string.sort_newest,
        BudgetDetailState.ListOrder.AMOUNT_DESCENDANT to Res.string.sort_amount_descending,
        BudgetDetailState.ListOrder.AMOUNT_ASCENDANT to Res.string.sort_amount_ascending
    )
    val currentLabel: StringResource = options.first { it.first == listOrder }.second

    Box {
        FilterChip(
            selected = listOrder != BudgetDetailState.ListOrder.DEFAULT,
            onClick = { expanded = true },
            label = { Text(stringResource(currentLabel), fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(Res.string.sort_entries),
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            },
            shape = MaterialTheme.shapes.small
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = AppColors.surfaceContainer
        ) {
            options.forEach { (order, label) ->
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

@Composable
private fun DayHeader(
    date: String,
    net: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = dayHeaderLabel(date),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.onSurfaceVariant
        )
        Text(
            text = net.toCurrency(),
            fontSize = 13.sp,
            color = AppColors.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    entry: BudgetEntry,
    index: Int,
    showDate: Boolean,
    showAuthor: Boolean,
    showSyncState: Boolean,
    isSelectionActive: Boolean,
    onIntent: (BudgetDetailIntent) -> Unit
) {
    val onItemChecked = { isChecked: Boolean ->
        onIntent(BudgetDetailIntent.ToggleSelectEntry(index, isChecked))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = {
                    onIntent(
                        if (!isSelectionActive) BudgetDetailIntent.ShowEntry(entry)
                        else BudgetDetailIntent.ToggleSelectEntry(index, !entry.isSelected)
                    )
                },
                onLongClick = {
                    onIntent(BudgetDetailIntent.ToggleSelectionState(true))
                    onItemChecked(true)
                }
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionActive) {
            Checkbox(
                checked = entry.isSelected,
                onCheckedChange = onItemChecked,
                modifier = Modifier.size(36.dp)
            )
        } else {
            CategoryBadge(category = entry.category)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.description.ifBlank { stringResource(Res.string.no_description) },
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            EntryDetailsLine(
                entry = entry,
                showDate = showDate,
                showAuthor = showAuthor,
                showSyncState = showSyncState
            )
        }

        val isOutcome = entry.type == BudgetEntry.Type.OUTCOME
        Text(
            text = (if (isOutcome) "-" else "+") + entry.amount.toCurrency(),
            color = if (isOutcome) AppColors.error else green_success,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(min = 72.dp),
            maxLines = 1,
            textAlign = TextAlign.End
        )
    }
}

/** "Comida · ana · Pendiente": the category, then only what applies. */
@Composable
private fun EntryDetailsLine(
    entry: BudgetEntry,
    showDate: Boolean,
    showAuthor: Boolean,
    showSyncState: Boolean
) {
    val isWaiting = entry.category == BudgetEntry.Category.UNCATEGORIZED
    val parts = buildList {
        if (showDate) add(shortDateLabel(entry.date) to AppColors.onSurfaceVariant)
        add(entry.category.toStringResource() to if (isWaiting) AppColors.tertiary else AppColors.onSurfaceVariant)
        if (showAuthor) {
            entry.createdByEmail?.takeIf { it.isNotBlank() }?.let {
                add(it.substringBefore('@') to AppColors.onSurfaceVariant)
            }
        }
        if (showSyncState && !entry.isSynced) add(stringResource(Res.string.not_synced) to AppColors.error)
    }

    Row(
        modifier = Modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        parts.forEachIndexed { i, (text, color) ->
            if (i > 0) DetailSeparator()
            Text(
                text = text,
                fontSize = 12.sp,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailSeparator() {
    Text(
        text = " · ",
        fontSize = 12.sp,
        color = AppColors.onSurfaceVariant
    )
}

@Composable
private fun SelectionHeader(
    entries: List<BudgetEntry>,
    onIntent: (BudgetDetailIntent) -> Unit
) {
    Column(modifier = Modifier.background(AppColors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = entries.all { it.isSelected },
                onCheckedChange = { onIntent(BudgetDetailIntent.ToggleAllEntriesSelection(it)) }
            )
            Text(
                text = stringResource(Res.string.selected_entries, entries.count { it.isSelected }.toString()),
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                modifier = Modifier.offset(10.dp),
                onClick = { onIntent(BudgetDetailIntent.ToggleSelectionState(false)) }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.close_entries_selection_mode)
                )
            }
        }
        DefDivider()
    }
}

@Composable
private fun DeleteButton(
    onIntent: (BudgetDetailIntent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background)
            .padding(16.dp)
    ) {
        Button(
            onClick = { onIntent(BudgetDetailIntent.ToggleDeleteEntriesModal(true)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.error,
                contentColor = AppColors.onError
            )
        ) {
            Text(text = stringResource(Res.string.delete))
        }
    }
}

private fun List<BudgetEntry>.sumOf(type: BudgetEntry.Type): Double =
    filter { it.type == type }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
