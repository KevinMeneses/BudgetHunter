package com.meneses.budgethunter.insAndOuts.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.insAndOuts.domain.BudgetItem
import com.meneses.budgethunter.commons.ui.DefDivider
import com.meneses.budgethunter.theme.AppColors

@Composable
fun InsAndOutsContent(
    budgetAmount: Double,
    budgetItems: List<BudgetItem>,
    paddingValues: PaddingValues,
    onBudgetClick: () -> Unit,
    onItemClick: (BudgetItem) -> Unit
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
                budgetItems = budgetItems,
                onItemClick = onItemClick
            )
            Spacer(modifier = Modifier.height(20.dp))
            FooterSection(
                budgetItems = budgetItems,
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
    budgetItems: List<BudgetItem>,
    onItemClick: (BudgetItem) -> Unit
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
            items(budgetItems.size) { index ->
                val budgetItem = budgetItems[index]
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(budgetItem) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(text = budgetItem.description ?: "Sin descripcion")
                    val operatorSign =
                        if (budgetItem.type == BudgetItem.Type.OUTCOME) "-"
                        else EMPTY
                    Text(text = operatorSign + budgetItem.amount.toString())
                }
                if (index != budgetItems.size - 1) DefDivider()
            }
        }
    }
}

@Composable
fun FooterSection(
    budgetItems: List<BudgetItem>,
    budgetAmount: Double
) {
    val outcomes = budgetItems
        .filter { it.type == BudgetItem.Type.OUTCOME }
        .map { it.amount }
        .reduceOrNull { acc, actual -> acc + actual }
        ?: 0.0

    val incomes = budgetItems
        .filter { it.type == BudgetItem.Type.INCOME }
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
        AmountText("Total gastos:", "$operatorSign$outcomes")
        DefDivider(color = AppColors.onSecondaryContainer)
        AmountText("Balance:", balance.toString())
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


