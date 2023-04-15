package com.meneses.budgethunter.detail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.detail.DetailViewModel
import com.meneses.budgethunter.fakeNavigation
import com.meneses.budgethunter.insAndOuts.domain.BudgetItem
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
    budgetItem: BudgetItem? = null,
    myViewModel: DetailViewModel = viewModel()
) {
    val uiState by myViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (budgetItem != null && uiState.detail?.id != budgetItem.id) {
            myViewModel.setDetail(budgetItem)
        }
    }

    Scaffold(
        topBar = {
            val title = remember {
                if (budgetItem == null) "Nuevo presupuesto"
                else "Modificar presupuesto"
            }

            AppBar(
                title = title,
                leftButtonIcon = Icons.Default.ArrowBack,
                rightButtonIcon = Icons.Default.Done,
                onLeftButtonClick = { navigator.popBackStack() },
                onRightButtonClick = {
                    navigator.popBackStack()
                }
            )
        }
    ) {
        DetailForm(budgetItem, it)
    }
}
