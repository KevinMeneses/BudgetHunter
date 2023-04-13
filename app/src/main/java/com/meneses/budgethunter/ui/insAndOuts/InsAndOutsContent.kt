package com.meneses.budgethunter.ui.insAndOuts

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
import com.meneses.budgethunter.model.BudgetDetail
import com.meneses.budgethunter.ui.budgetDetailLists
import com.meneses.budgethunter.ui.commons.DefDivider
import com.meneses.budgethunter.ui.destinations.DetailScreenDestination
import com.meneses.budgethunter.ui.theme.AppColors
import com.meneses.budgethunter.ui.totalIncome
import com.ramcosta.composedestinations.navigation.DestinationsNavigator


@Composable
fun InsAndOutsContent(
    navigator: DestinationsNavigator,
    paddingValues: PaddingValues,
    onBudgetClick: () -> Unit
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
            BudgetSection(onClick = onBudgetClick)
            Spacer(modifier = Modifier.height(20.dp))
            ListSection(navigator)
            Spacer(modifier = Modifier.height(20.dp))
            FooterSection()
        }
    }
}

@Composable
fun BudgetSection(
    onClick: () -> Unit
) {
    val budget = remember {
        totalIncome.toBigDecimal().toPlainString()
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
                text = "Presupuesto\n$budget",
                modifier = Modifier.padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.onTertiaryContainer
            )
        }
    }

}

@Composable
private fun ColumnScope.ListSection(navigator: DestinationsNavigator) {
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
            items(budgetDetailLists.size) { index ->
                val budgetDetail = remember { budgetDetailLists[index] }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navigator.navigate(DetailScreenDestination(budgetDetail))
                        }
                        .padding(vertical = 10.dp)
                ) {
                    Text(text = budgetDetail.description ?: "Sin descripcion")
                    Text(text = budgetDetail.amount.toString())
                }
                if (index != budgetDetailLists.size - 1) DefDivider()
            }
        }
    }
}

@Composable
fun FooterSection() {
    val totalBudget = remember {
        val listFiltered = budgetDetailLists.filter { it.type == BudgetDetail.Type.OUTCOME }.map { it.amount }
        listFiltered.reduce { acc, actual -> acc + actual }.toString()
    }
    val balance = remember {
        (totalIncome - totalBudget.toDouble()).toString()
    }

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
        AmountText("Total gastos:", totalBudget)
        DefDivider(color = AppColors.onSecondaryContainer)
        AmountText("Balance:", balance)
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


