package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.budget_options
import budgethunter.composeapp.generated.resources.change_name
import budgethunter.composeapp.generated.resources.created
import budgethunter.composeapp.generated.resources.delete
import budgethunter.composeapp.generated.resources.delete_budget
import budgethunter.composeapp.generated.resources.duplicate
import budgethunter.composeapp.generated.resources.duplicate_budget
import budgethunter.composeapp.generated.resources.edit_budget
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.CompottiePlaceholder
import com.meneses.budgethunter.commons.ui.LoadingScreen
import com.meneses.budgethunter.commons.util.toCurrency
import com.meneses.budgethunter.theme.AppColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

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
                .padding(bottom = 90.dp),
            verticalArrangement = if (list.isEmpty()) Arrangement.Center else Arrangement.Top
        ) {
            if (list.isEmpty()) {
                item {
                    CompottiePlaceholder(
                        fileName = "empty_state.json",
                        modifier = Modifier.fillMaxSize()
                    )
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
            defaultElevation = 4.dp
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
                .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val date = try {
                    // Simple date parsing for now, can be improved later
                    val parsedDate = LocalDate.parse(budget.date)
                    "${parsedDate.dayOfMonth}/${parsedDate.monthNumber}/${parsedDate.year}"
                } catch (_: Exception) {
                    budget.date
                }

                Text(
                    text = stringResource(Res.string.created, date),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = getExpensesWithBoldSlash(budget),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            var dropdownExpanded by remember {
                mutableStateOf(false)
            }

            IconButton(
                onClick = { dropdownExpanded = true },
                content = {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(Res.string.budget_options),
                        tint = MaterialTheme.colorScheme.onSurface
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
                                        contentDescription = stringResource(Res.string.edit_budget),
                                        modifier = Modifier.size(18.dp),
                                        tint = AppColors.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(Res.string.change_name),
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
                                        contentDescription = stringResource(Res.string.duplicate_budget),
                                        modifier = Modifier.size(18.dp),
                                        tint = AppColors.secondary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(Res.string.duplicate),
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
                                        contentDescription = stringResource(Res.string.delete_budget),
                                        modifier = Modifier.size(18.dp),
                                        tint = AppColors.error
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(Res.string.delete),
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

@Composable
private fun getExpensesWithBoldSlash(budget: Budget): AnnotatedString {
    return buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            )
        ) {
            append(budget.totalExpenses.toCurrency())
        }
        withStyle(
            style = SpanStyle(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            )
        ) {
            append(" / ")
        }
        withStyle(
            style = SpanStyle(
                fontSize = MaterialTheme.typography.bodySmall.fontSize
            )
        ) {
            append(budget.amount.toCurrency())
        }
    }
}
