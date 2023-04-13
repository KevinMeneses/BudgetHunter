package com.meneses.budgethunter.ui.budgetList

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
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.ui.budgetList
import com.meneses.budgethunter.ui.commons.EMPTY
import com.meneses.budgethunter.ui.commons.dashedBorder
import com.meneses.budgethunter.ui.theme.AppColors

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BudgetListContent(
    paddingValues: PaddingValues,
    onBudgetClick: (Int) -> Unit,
    onAddBudgetClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .padding(20.dp)
    ) {
        items(budgetList.size) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.tertiaryContainer,
                    contentColor = AppColors.onTertiaryContainer
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 5.dp
                ),
                onClick = { onBudgetClick(it) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(text = budgetList[it].name)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Card(
                colors = CardDefaults.outlinedCardColors(),
                modifier = Modifier.dashedBorder(
                    width = 1.dp,
                    color = AppColors.onSecondaryContainer,
                    shape = AbsoluteRoundedCornerShape(15.dp),
                    on = 10.dp,
                    off = 8.dp
                ),
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
    }
}