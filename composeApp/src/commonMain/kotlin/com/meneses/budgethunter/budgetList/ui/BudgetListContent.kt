package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.available_label
import budgethunter.composeapp.generated.resources.budget_options
import budgethunter.composeapp.generated.resources.not_synced
import budgethunter.composeapp.generated.resources.notification_reader_stopped_banner
import budgethunter.composeapp.generated.resources.over_budget
import budgethunter.composeapp.generated.resources.percent_of_total
import budgethunter.composeapp.generated.resources.shared_with_people
import budgethunter.composeapp.generated.resources.spent_amount
import com.meneses.budgethunter.budgetList.application.BudgetListIntent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.CompottiePlaceholder
import com.meneses.budgethunter.commons.ui.LoadingScreen
import com.meneses.budgethunter.commons.ui.OfflineBanner
import com.meneses.budgethunter.commons.ui.SpendingProgressBar
import com.meneses.budgethunter.commons.ui.StatusChip
import com.meneses.budgethunter.commons.ui.WarningBanner
import com.meneses.budgethunter.commons.ui.fadingBottomEdge
import com.meneses.budgethunter.commons.util.toCurrency
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListContent(
    list: List<Budget>,
    isLoading: Boolean,
    isSyncing: Boolean,
    isOnline: Boolean,
    showSyncState: Boolean,
    isNotificationReaderStopped: Boolean,
    onNotificationReaderStoppedClick: () -> Unit,
    paddingValues: PaddingValues,
    onIntent: (BudgetListIntent) -> Unit
) {
    if (isLoading) {
        LoadingScreen()
    } else {
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { BudgetListIntent.SyncBudgets.run(onIntent) },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column {
                OfflineBanner(isOffline = !isOnline)
                // Settings holds the explanation and the way to the force stop
                WarningBanner(
                    visible = isNotificationReaderStopped,
                    text = stringResource(Res.string.notification_reader_stopped_banner),
                    onClick = onNotificationReaderStoppedClick
                )
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .fadingBottomEdge(visible = listState.canScrollForward),
                    // Room at the bottom so the last card can scroll above the floating button
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = if (list.isEmpty()) Arrangement.Center else Arrangement.spacedBy(12.dp)
                ) {
                    if (list.isEmpty()) {
                        item {
                            CompottiePlaceholder(
                                fileName = "empty_state.json",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        items(list, key = { it.id }) { budget ->
                            BudgetItem(
                                budget = budget,
                                showSyncState = showSyncState,
                                onIntent = onIntent
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One budget, answering "how much is left?" first: the money still available is the big number,
 * the bar and the line under it say how much of it is already spent.
 */
@Composable
private fun BudgetItem(
    budget: Budget,
    showSyncState: Boolean,
    onIntent: (BudgetListIntent) -> Unit
) {
    val remaining = budget.totalAvailable - budget.totalExpenses
    val isOver = remaining < 0
    val spentFraction = when {
        budget.totalAvailable > 0 -> (budget.totalExpenses / budget.totalAvailable).toFloat()
        budget.totalExpenses > 0 -> 1f
        else -> 0f
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = AppColors.surfaceContainer,
            contentColor = AppColors.onSurface
        ),
        shape = MaterialTheme.shapes.large,
        onClick = {
            BudgetListIntent
                .OpenBudget(budget)
                .run(onIntent)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = budget.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isOver) {
                    StatusChip(
                        text = stringResource(Res.string.over_budget),
                        icon = Icons.Default.Warning,
                        containerColor = AppColors.errorContainer,
                        contentColor = AppColors.onErrorContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Only the exception is shown: a synced budget carries no icon at all
                if (showSyncState && !budget.isSynced) {
                    StatusChip(
                        text = stringResource(Res.string.not_synced),
                        icon = Icons.Default.CloudQueue,
                        containerColor = AppColors.surfaceContainerHighest,
                        contentColor = AppColors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                BudgetItemMenuButton(budget = budget, onIntent = onIntent)
            }

            Row(
                modifier = Modifier.padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.available_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // Like the chips, only the exception: a budget nobody else sees says nothing
                if (budget.collaboratorCount > 1) {
                    SharedIndicator(count = budget.collaboratorCount)
                }
            }
            Text(
                text = remaining.toCurrency(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOver) AppColors.error else AppColors.onSurface,
                maxLines = 1
            )

            if (budget.totalAvailable > 0 || budget.totalExpenses > 0) {
                SpendingProgressBar(
                    fraction = spentFraction.coerceIn(0f, 1f),
                    isOver = isOver,
                    modifier = Modifier.padding(top = 10.dp, end = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // When both don't fit, the spent amount wraps and the percentage stays on one line
                    Text(
                        text = stringResource(Res.string.spent_amount, budget.totalExpenses.toCurrency()),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (budget.totalAvailable > 0) {
                        Text(
                            text = stringResource(
                                Res.string.percent_of_total,
                                "${(spentFraction * 100).roundToInt()}%",
                                budget.totalAvailable.toCurrency()
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/** People icon and how many share the budget, the user included; read as one sentence. */
@Composable
private fun SharedIndicator(count: Int) {
    val description = stringResource(Res.string.shared_with_people, count)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = AppColors.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetItemMenuButton(
    budget: Budget,
    onIntent: (BudgetListIntent) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    IconButton(onClick = { dropdownExpanded = true }) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(Res.string.budget_options),
            tint = AppColors.onSurfaceVariant
        )

        BudgetListItemMenu(
            dropdownExpanded = dropdownExpanded,
            onDismiss = { dropdownExpanded = false },
            onUpdateClick = {
                BudgetListIntent
                    .ToggleUpdateModal(budget)
                    .run(onIntent)
            },
            onDuplicateClick = {
                BudgetListIntent
                    .DuplicateBudget(budget)
                    .run(onIntent)
            },
            onDeleteClick = {
                BudgetListIntent
                    .DeleteBudget(budget.id.toLong())
                    .run(onIntent)
            }
        )
    }
}
