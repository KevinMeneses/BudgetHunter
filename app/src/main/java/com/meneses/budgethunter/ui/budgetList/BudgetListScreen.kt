package com.meneses.budgethunter.ui.budgetList

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.meneses.budgethunter.ui.budgetList
import com.meneses.budgethunter.ui.commons.AppBar
import com.meneses.budgethunter.ui.destinations.InsAndOutsScreenDestination
import com.meneses.budgethunter.ui.fakeNavigation
import com.meneses.budgethunter.ui.theme.BudgetHunterTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetHunterTheme {
        BudgetListScreen(fakeNavigation)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination
fun BudgetListScreen(
    navigator: DestinationsNavigator
) {
    var createModalVisibility by remember {
        mutableStateOf(false)
    }

    var filterModalVisibility by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            AppBar(
                title = "Presupuestos",
                rightButtonIcon = Icons.Default.Search,
                onRightButtonClick = { filterModalVisibility = true }
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            BudgetListContent(
                paddingValues = it,
                onBudgetClick = {
                    navigator.navigate(
                        InsAndOutsScreenDestination(budgetList[it])
                    )
                },
                onAddBudgetClick = { createModalVisibility = true }
            )

            NewBudgetModal(
                show = createModalVisibility,
                onDismiss = { createModalVisibility = false },
                onButtonClick = {
                    navigator.navigate(
                        InsAndOutsScreenDestination(it)
                    )
                }
            )

            FilterListModal(
                show = filterModalVisibility,
                onDismiss = { filterModalVisibility = false },
                onClear = { },
                onButtonClick = { }
            )
        }
    }
}