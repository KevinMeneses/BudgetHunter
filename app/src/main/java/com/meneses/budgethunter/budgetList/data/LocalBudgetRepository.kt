package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetListMock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class LocalBudgetRepository: BudgetRepository {

    override val budgetList: Flow<List<Budget>> get() = _budgetList
    private val _budgetList = MutableStateFlow<List<Budget>>(emptyList())

    override fun getAllBudgets() {
        _budgetList.value = budgetListMock
    }

    override fun getBudgetsBy(budget: Budget) {
        val list = budgetListMock
            .filter {
                if (budget.name.isBlank()) true
                else it.name == budget.name
            }
            .filter {
                if (budget.frequency == null) true
                else it.frequency == budget.frequency
            }

        _budgetList.value = list
    }

    override fun createBudget(budget: Budget): Budget {
        val id = budgetListMock.size
        val budgetToSave = budget.copy(id = id)
        budgetListMock.add(budgetToSave)
        return budgetToSave
    }

    override fun updateBudget(budget: Budget) {
        budgetListMock.removeIf { it.id == budget.id }
        budgetListMock.add(budget.id, budget)
    }

    override fun deleteBudget(budget: Budget) {
        budgetListMock.remove(budget)
    }
}