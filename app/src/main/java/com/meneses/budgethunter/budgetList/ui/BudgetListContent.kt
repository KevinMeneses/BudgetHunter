package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.LoadingScreen
import com.meneses.budgethunter.commons.ui.LottiePlaceholder
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.BudgetHunterTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetHunterTheme {
        BudgetListContent(
            list = listOf(
                Budget(name = "Noviembre"),
                Budget(name = "Diciembre"),
                Budget(name = "Enero"),
            ),
            isLoading = false,
            paddingValues = PaddingValues(),
            onEvent = {}
        )
    }
}

@Composable
fun BudgetListContent(
    list: List<Budget>,
    isLoading: Boolean,
    paddingValues: PaddingValues,
    onEvent: (BudgetListEvent) -> Unit
) {
    if (isLoading) {
        LoadingScreen()
    } else {
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 5.dp)
                .padding(bottom = 90.dp)
        ) {
            if (list.isEmpty()) {
                item {
                    LottiePlaceholder(resId = R.raw.empty_state)
                }
            } else {
                items(list.size) {
                    Spacer(Modifier.size(10.dp))
                    BudgetItem(
                        budget = list[it],
                        onEvent = onEvent
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

@Composable
private fun BudgetItem(
    budget: Budget,
    onEvent: (BudgetListEvent) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = AppColors.tertiaryContainer,
            contentColor = AppColors.onTertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        ),
        onClick = {
            BudgetListEvent
                .OpenBudget(budget)
                .run(onEvent)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var dropdownExpanded by remember {
                mutableStateOf(false)
            }

            Text(text = budget.name)
            IconButton(
                onClick = { dropdownExpanded = true },
                content = {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.budget_list_icon_description)
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.surface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.change_name_icon_description),
                                        modifier = Modifier.size(18.dp),
                                        tint = AppColors.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.change_name),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
                                BudgetListEvent
                                    .ToggleUpdateModal(budget)
                                    .run(onEvent)
                            }
                        )
                        
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.duplicate_icon_description),
                                        modifier = Modifier.size(18.dp),
                                        tint = AppColors.secondary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.duplicate),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
                                BudgetListEvent
                                    .DuplicateBudget(budget)
                                    .run(onEvent)
                            }
                        )
                        
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete_icon_description),
                                        modifier = Modifier.size(18.dp),
                                        tint = AppColors.error
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.delete),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.error
                                    )
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
                                BudgetListEvent
                                    .DeleteBudget(budget.id.toLong())
                                    .run(onEvent)
                            }
                        )
                    }
                }
            )
        }
    }
}
