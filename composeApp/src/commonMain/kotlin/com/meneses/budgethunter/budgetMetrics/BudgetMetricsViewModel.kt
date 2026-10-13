package com.meneses.budgethunter.budgetMetrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsIntent
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsState
import com.meneses.budgethunter.budgetMetrics.application.CategorizationOutcome
import com.meneses.budgethunter.budgetMetrics.application.CategorizeEntriesUseCase
import com.meneses.budgethunter.budgetMetrics.application.GetCategorizationOfferUseCase
import com.meneses.budgethunter.budgetMetrics.application.GetTotalsPerCategoryUseCase
import com.meneses.budgethunter.budgetMetrics.domain.CategoryMetric
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetMetricsViewModel(
    private val budget: Budget,
    private val getTotalsPerCategoryUseCase: GetTotalsPerCategoryUseCase,
    private val getCategorizationOfferUseCase: GetCategorizationOfferUseCase,
    private val categorizeEntriesUseCase: CategorizeEntriesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(BudgetMetricsState())
    val uiState = _uiState.asStateFlow()

    private var metricsJob: Job? = null

    init {
        getMetrics(_uiState.value.selectedType)
        loadCategorizationOffer()
    }

    fun sendIntent(intent: BudgetMetricsIntent) {
        when (intent) {
            is BudgetMetricsIntent.SelectType -> selectType(intent.type)
            BudgetMetricsIntent.ConfirmCategorization -> categorize()
            BudgetMetricsIntent.DeclineCategorization -> declineCategorization()
            BudgetMetricsIntent.OutcomeShown -> _uiState.update { it.copy(categorizationOutcome = null) }
        }
    }

    private fun selectType(type: BudgetEntry.Type) {
        if (type == _uiState.value.selectedType) return
        _uiState.update { it.copy(selectedType = type, isLoading = true) }
        getMetrics(type)
    }

    private fun getMetrics(type: BudgetEntry.Type) {
        // Only the last type asked for may land in the state, even if an older read ends later.
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch {
            val totalsPerCategory = getTotalsPerCategoryUseCase.execute(type)
            val total = totalsPerCategory.values.sum()

            val categoryMetrics = totalsPerCategory.map { (category, amount) ->
                CategoryMetric(
                    category = category,
                    amount = amount,
                    percentage = if (total == 0.0) 0.0 else (amount * 100) / total
                )
            }

            _uiState.update {
                it.copy(
                    categoryMetrics = categoryMetrics,
                    total = total,
                    isLoading = false
                )
            }
        }
    }

    private fun loadCategorizationOffer() {
        viewModelScope.launch {
            val waiting = getCategorizationOfferUseCase.execute(budget)
            _uiState.update { it.copy(categorizationOffer = waiting.takeIf { count -> count > 0 }) }
        }
    }

    private fun declineCategorization() {
        val waiting = _uiState.value.categorizationOffer ?: return
        getCategorizationOfferUseCase.decline(budget, waiting)
        _uiState.update { it.copy(categorizationOffer = null) }
    }

    private fun categorize() {
        if (_uiState.value.isCategorizing || _uiState.value.categorizationOffer == null) return
        _uiState.update { it.copy(isCategorizing = true) }

        viewModelScope.launch {
            val outcome = categorizeEntriesUseCase.execute(budget)
            if (outcome is CategorizationOutcome.Done) getCategorizationOfferUseCase.clearDecline(budget)

            // After a run the dialog is only offered again for what is still waiting, if anything
            // can be done about it: a run that hit an error keeps the offer, so it can be retried.
            val offer = if (outcome is CategorizationOutcome.Failed) _uiState.value.categorizationOffer else null
            _uiState.update {
                it.copy(
                    isCategorizing = false,
                    categorizationOffer = offer,
                    categorizationOutcome = outcome
                )
            }
            getMetrics(_uiState.value.selectedType)
        }
    }
}
