package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.budgets
import budgethunter.composeapp.generated.resources.new_budget
import budgethunter.composeapp.generated.resources.search
import com.meneses.budgethunter.budgetList.application.BudgetListIntent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.commons.platform.NetworkMonitor
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource
import kotlinx.serialization.Serializable

@Serializable
object BudgetListScreen {
    @Composable
    fun Show(
        uiState: BudgetListState,
        onIntent: (BudgetListIntent) -> Unit,
        showSettings: () -> Unit,
        networkMonitor: NetworkMonitor,
        snackbarHostState: SnackbarHostState
    ) {
        var dropdownExpanded by remember { mutableStateOf(false) }
        // Back from the system settings or from the background: the notification reader may have stopped
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            BudgetListIntent.CheckNotificationReader.run(onIntent)
        }
        val isOnline by networkMonitor.isOnline.collectAsState()

        Scaffold(
            topBar = {
                if (uiState.isSearchMode) {
                    SearchAppBar(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = { query ->
                            BudgetListIntent
                                .UpdateSearchQuery(query)
                                .run(onIntent)
                        },
                        onBackClick = {
                            BudgetListIntent
                                .ToggleSearchMode(false)
                                .run(onIntent)
                        }
                    )
                } else {
                    AppBar(
                        title = stringResource(Res.string.budgets),
                        secondRightButtonIcon = Icons.Default.Search,
                        secondRightButtonDescription = stringResource(Res.string.search),
                        onSecondRightButtonClick = {
                            BudgetListIntent
                                .ToggleSearchMode(true)
                                .run(onIntent)
                        },
                        trailingContent = {
                            Box {
                                AccountButton(
                                    email = uiState.accountEmail,
                                    // Signed in, Settings holds everything about the account,
                                    // signing out included; offline, the menu offers to sign in
                                    onClick = {
                                        if (uiState.isAuthenticated) showSettings() else dropdownExpanded = true
                                    }
                                )
                                BudgetListMenu(
                                    expanded = dropdownExpanded,
                                    onDismiss = { dropdownExpanded = false },
                                    onSettingsClick = showSettings,
                                    onSignInClick = { BudgetListIntent.SignIn.run(onIntent) }
                                )
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text(text = stringResource(Res.string.new_budget)) },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    containerColor = AppColors.primaryContainer,
                    contentColor = AppColors.onPrimaryContainer,
                    onClick = {
                        BudgetListIntent
                            .ToggleAddModal(true)
                            .run(onIntent)
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            BudgetListContent(
                list = uiState.budgetList,
                isLoading = uiState.isLoading,
                isSyncing = uiState.isSyncing,
                isOnline = if (uiState.isAuthenticated) isOnline else true,
                showSyncState = uiState.isAuthenticated,
                isNotificationReaderStopped = uiState.isNotificationReaderStopped,
                onNotificationReaderStoppedClick = showSettings,
                paddingValues = paddingValues,
                onIntent = onIntent
            )
        }

        NewBudgetModal(
            show = uiState.addModalVisibility,
            onIntent = onIntent,
            isCreating = uiState.isCreatingBudget
        )

        UpdateBudgetModal(
            budget = uiState.budgetToUpdate,
            onIntent = onIntent,
            isUpdating = uiState.isUpdatingBudget
        )
    }
}
