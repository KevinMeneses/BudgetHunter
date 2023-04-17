package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.ui.DefDivider
import com.meneses.budgethunter.theme.AppColors

@Composable
fun BudgetDetailContent(
    budgetAmount: Double,
    budgetEntries: List<BudgetEntry>,
    paddingValues: PaddingValues,
    onBudgetClick: () -> Unit,
    onItemClick: (BudgetEntry) -> Unit
) {
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
                amount = budgetAmount,
                onClick = onBudgetClick
            )
            Spacer(modifier = Modifier.height(20.dp))
            ListSection(
                budgetEntries = budgetEntries,
                onItemClick = onItemClick
            )
            Spacer(modifier = Modifier.height(20.dp))
            BalanceSection(
                budgetEntries = budgetEntries,
                budgetAmount = budgetAmount
            )
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

@Composable
private fun ColumnScope.ListSection(
    budgetEntries: List<BudgetEntry>,
    onItemClick: (BudgetEntry) -> Unit
) {
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
            items(budgetEntries.size) { index ->
                val budgetItem = budgetEntries[index]
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(budgetItem) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(text = budgetItem.description.takeIf { !it.isNullOrBlank() } ?: "Sin descripcion")
                    val operatorSign =
                        if (budgetItem.type == BudgetEntry.Type.OUTCOME) "-"
                        else EMPTY
                    Text(text = operatorSign + budgetItem.amount.toString())
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
        .reduceOrNull { acc, actual -> (acc ?: 0.0) + (actual ?: 0.0) }
        ?: 0.0

    val incomes = budgetEntries
        .filter { it.type == BudgetEntry.Type.INCOME }
        .map { it.amount }
        .reduceOrNull { acc, actual -> (acc ?: 0.0) + (actual ?: 0.0) }
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


