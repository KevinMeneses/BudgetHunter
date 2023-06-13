package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter

sealed interface BudgetListEvent {
    data class CreateBudget(val budget: Budget) : BudgetListEvent
    data class OpenBudget(val budget: Budget) : BudgetListEvent
    data class FilterList(val filter: BudgetFilter) : BudgetListEvent
    data class ToggleAddModal(val isVisible: Boolean) : BudgetListEvent
    data class ToggleFilterModal(val isVisible: Boolean) : BudgetListEvent
    object ClearFilter : BudgetListEvent
    object ClearNavigation : BudgetListEvent
}
