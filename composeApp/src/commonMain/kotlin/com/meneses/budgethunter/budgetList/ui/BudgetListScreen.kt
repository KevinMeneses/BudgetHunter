package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.budgets
import budgethunter.composeapp.generated.resources.create_new_budget
import budgethunter.composeapp.generated.resources.open_menu
import budgethunter.composeapp.generated.resources.search
import com.meneses.budgethunter.budgetList.application.BudgetListIntent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.commons.platform.NetworkMonitor
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.LoadingOverlay
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.theme.AppColors
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

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
                        leftButtonIcon = Icons.Default.Search,
                        rightButtonIcon = Icons.Default.MoreVert,
                        leftButtonDescription = stringResource(Res.string.search),
                        rightButtonDescription = stringResource(Res.string.open_menu),
                        onLeftButtonClick = {
                            BudgetListIntent
                                .ToggleSearchMode(true)
                                .run(onIntent)
                        },
                        onRightButtonClick = {
                            dropdownExpanded = true
                        },
                        animateRightButton = uiState.filter != null,
                        rightButtonDropdownContent = {
                            BudgetListMenu(
                                expanded = dropdownExpanded,
                                onDismiss = { dropdownExpanded = false },
                                onSettingsClick = showSettings,
                                isAuthenticated = uiState.isAuthenticated,
                                onSignOutClick = { BudgetListIntent.SignOut.run(onIntent) },
                                onSignInClick = { BudgetListIntent.SignIn.run(onIntent) }
                            )
                        }
                    )
                }
            },
            floatingActionButton = {
                // An empty slot where the next budget goes: dashed outline and a faint tint, flat,
                // so it reads as an action and not as one more budget card.
                FloatingActionButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .dashedBorder(
                            width = 1.5.dp,
                            color = AppColors.primary,
                            shape = AbsoluteRoundedCornerShape(10.dp),
                            on = 8.dp,
                            off = 6.dp
                        ),
                    shape = AbsoluteRoundedCornerShape(10.dp),
                    containerColor = AppColors.primary.copy(alpha = 0.08f).compositeOver(AppColors.surface),
                    contentColor = AppColors.primary,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    onClick = {
                        BudgetListIntent
                            .ToggleAddModal(true)
                            .run(onIntent)
                    }
                ) {
                    Text(
                        modifier = Modifier.padding(vertical = 20.dp),
                        text = stringResource(Res.string.create_new_budget),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            BudgetListContent(
                list = uiState.budgetList,
                isLoading = uiState.isLoading,
                isSyncing = uiState.isSyncing,
                isOnline = if (uiState.isAuthenticated) isOnline else true,
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

        if (uiState.isSigningOut) {
            LoadingOverlay()
        }
    }
}
