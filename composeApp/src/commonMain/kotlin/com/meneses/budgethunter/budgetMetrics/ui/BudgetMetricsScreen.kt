package com.meneses.budgethunter.budgetMetrics.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.categorize_ai_off
import budgethunter.composeapp.generated.resources.categorize_already_running
import budgethunter.composeapp.generated.resources.categorize_done
import budgethunter.composeapp.generated.resources.categorize_done_pending
import budgethunter.composeapp.generated.resources.come_back
import budgethunter.composeapp.generated.resources.error_unknown
import budgethunter.composeapp.generated.resources.metrics_expenses
import budgethunter.composeapp.generated.resources.metrics_incomes
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsIntent
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsState
import com.meneses.budgethunter.budgetMetrics.application.CategorizationOutcome
import com.meneses.budgethunter.commons.data.network.ApiError
import com.meneses.budgethunter.commons.ui.AppBar
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
data class BudgetMetricsScreen(val budget: Budget) {

    @Composable
    fun Show(
        uiState: BudgetMetricsState,
        onIntent: (BudgetMetricsIntent) -> Unit,
        goBack: () -> Unit
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val outcome = uiState.categorizationOutcome
        if (outcome != null) {
            val message = outcome.toMessage()
            LaunchedEffect(outcome) {
                snackbarHostState.showSnackbar(message)
                onIntent(BudgetMetricsIntent.OutcomeShown)
            }
        }

        uiState.categorizationOffer?.let { waiting ->
            CategorizeEntriesDialog(
                waiting = waiting,
                isCategorizing = uiState.isCategorizing,
                onConfirm = { onIntent(BudgetMetricsIntent.ConfirmCategorization) },
                onDecline = { onIntent(BudgetMetricsIntent.DeclineCategorization) }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AppBar(
                    title = budget.name,
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    leftButtonDescription = stringResource(Res.string.come_back),
                    onLeftButtonClick = goBack
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues)
            ) {
                // Always shown, even with nothing to chart: it is the way back from a kind of
                // entry this budget happens to have none of.
                TypeSelector(
                    selectedType = uiState.selectedType,
                    onTypeSelected = { BudgetMetricsIntent.SelectType(it).run(onIntent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )

                BudgetMetricsContent(uiState = uiState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelector(
    selectedType: BudgetEntry.Type,
    onTypeSelected: (BudgetEntry.Type) -> Unit,
    modifier: Modifier = Modifier
) {
    val types = listOf(
        BudgetEntry.Type.OUTCOME to Res.string.metrics_expenses,
        BudgetEntry.Type.INCOME to Res.string.metrics_incomes
    )

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        types.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = type == selectedType,
                onClick = { onTypeSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
            ) {
                Text(text = stringResource(label))
            }
        }
    }
}

@Composable
private fun CategorizationOutcome.toMessage(): String = when (this) {
    is CategorizationOutcome.Done ->
        if (pending > 0) stringResource(Res.string.categorize_done_pending, categorized, pending)
        else stringResource(Res.string.categorize_done, categorized)
    CategorizationOutcome.AlreadyRunning -> stringResource(Res.string.categorize_already_running)
    CategorizationOutcome.AiOff -> stringResource(Res.string.categorize_ai_off)
    is CategorizationOutcome.Failed ->
        stringResource((error as? ApiError)?.messageResource ?: Res.string.error_unknown)
}

private fun BudgetMetricsIntent.run(onIntent: (BudgetMetricsIntent) -> Unit) = onIntent(this)
