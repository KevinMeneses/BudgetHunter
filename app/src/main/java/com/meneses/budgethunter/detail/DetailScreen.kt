package com.meneses.budgethunter.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.meneses.budgethunter.model.BudgetDetail
import com.meneses.budgethunter.commons.AppBar
import com.meneses.budgethunter.fakeNavigation
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    DetailScreen(fakeNavigation)
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun DetailScreen(
    navigator: DestinationsNavigator,
    budgetDetail: BudgetDetail? = null
) {
    Scaffold(
        topBar = {
            val title = remember {
                if (budgetDetail == null) "Nuevo presupuesto"
                else "Modificar presupuesto"
            }

            AppBar(
                title = title,
                leftButtonIcon = Icons.Default.ArrowBack,
                rightButtonIcon = Icons.Default.Done,
                onLeftButtonClick = { navigator.popBackStack() },
                onRightButtonClick = { navigator.popBackStack() }
            )
        }
    ) {
        DetailForm(budgetDetail, it)
    }
}
