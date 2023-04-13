package com.meneses.budgethunter.insAndOuts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.meneses.budgethunter.model.Budget
import com.meneses.budgethunter.commons.AppBar
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.destinations.DetailScreenDestination
import com.meneses.budgethunter.fakeNavigation
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    InsAndOutsScreen(fakeNavigation)
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun InsAndOutsScreen(
    navigator: DestinationsNavigator,
    budget: Budget? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var isFilterModalVisible by remember { mutableStateOf(false) }
    var isDeleteModalVisible by remember { mutableStateOf(false) }
    var isBudgetModalVisible by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerContent = {
            InsAndOutsMenu(
                onDeleteClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        isDeleteModalVisible = true
                    }
                },
                onFilterClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        isFilterModalVisible = true
                    }
                }
            )
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    title = budget?.name ?: EMPTY,
                    leftButtonIcon = Icons.Default.Menu,
                    rightButtonIcon = Icons.Default.Add,
                    onLeftButtonClick = {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    },
                    onRightButtonClick = {
                        navigator.navigate(DetailScreenDestination())
                    }
                )
            }
        ) {
            InsAndOutsContent(
                navigator = navigator,
                paddingValues = it,
                onBudgetClick = { isBudgetModalVisible = true }
            )
            FilterModal(
                show = isFilterModalVisible,
                onDismiss = { isFilterModalVisible = false },
                onClean = { },
                onApply = { }
            )
            DeleteConfirmationModal(
                show = isDeleteModalVisible,
                onDismiss = { isDeleteModalVisible = false },
                onAccept = { }
            )
            BudgetModal(
                show = isBudgetModalVisible,
                onDismiss = { isBudgetModalVisible = false },
                onSaveClick = { }
            )
        }
    }
}

