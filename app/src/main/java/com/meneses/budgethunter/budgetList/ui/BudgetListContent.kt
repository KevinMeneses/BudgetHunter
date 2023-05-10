package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.LottiePlaceholder
import com.meneses.budgethunter.commons.ui.blinkEffect
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.commons.ui.pulsateEffect
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.BudgetHunterTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetHunterTheme {
        BudgetListContent(
            list = emptyList(),
            paddingValues = PaddingValues(),
            animate = false,
            onBudgetClick = {},
            onAddBudgetClick = {}
        )
    }
}

@Composable
fun BudgetListContent(
    list: List<Budget>,
    paddingValues: PaddingValues,
    animate: Boolean,
    onBudgetClick: (Int) -> Unit,
    onAddBudgetClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .padding(20.dp)
    ) {
        if (list.isEmpty()) {
            item {
                LottiePlaceholder(resId = R.raw.empty_state)
                AddBudgetCard(onAddBudgetClick, animate)
            }
        } else {
            items(list.size) {
                BudgetItem(
                    budget = list[it],
                    onBudgetClick = { onBudgetClick(it) }
                )
            }
            item {
                AddBudgetCard(onAddBudgetClick, animate)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BudgetItem(
    budget: Budget,
    onBudgetClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = AppColors.tertiaryContainer,
            contentColor = AppColors.onTertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        ),
        onClick = { onBudgetClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(text = budget.name)
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetCard(
    onAddBudgetClick: () -> Unit,
    animate: Boolean
) {
    Card(
        colors = CardDefaults.outlinedCardColors(),
        modifier = Modifier
            .dashedBorder(
                width = 1.dp,
                color = AppColors.onSecondaryContainer,
                shape = AbsoluteRoundedCornerShape(15.dp),
                on = 10.dp,
                off = 8.dp
            )
            .pulsateEffect(animate)
            .blinkEffect(animate),
        onClick = onAddBudgetClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = EMPTY
            )
        }
    }
}