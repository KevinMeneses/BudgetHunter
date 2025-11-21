package com.meneses.budgethunter.fakes.repository

import com.meneses.budgethunter.budgetList.data.IBudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBudgetRepository : IBudgetRepository {
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    override val budgets: Flow<List<Budget>> = _budgets

    val createdBudgets = mutableListOf<Budget>()
    val updatedBudgets = mutableListOf<Budget>()
    private val budgetCache = mutableListOf<Budget>()

    fun emitBudgets(budgetList: List<Budget>) {
        budgetCache.clear()
        budgetCache.addAll(budgetList)
        _budgets.value = budgetList
    }

    override suspend fun create(budget: Budget): Budget {
        val newBudget = budget.copy(id = (budgetCache.maxOfOrNull { it.id } ?: 0) + 1)
        createdBudgets.add(newBudget)
        budgetCache.add(newBudget)
        _budgets.value = budgetCache.toList()
        return newBudget
    }

    override suspend fun update(budget: Budget) {
        updatedBudgets.add(budget)
        val index = budgetCache.indexOfFirst { it.id == budget.id }
        if (index >= 0) {
            budgetCache[index] = budget
            _budgets.value = budgetCache.toList()
        }
    }

    override suspend fun getById(id: Int): Budget? {
        return budgetCache.find { it.id == id }
    }

    override suspend fun getAllCached(): List<Budget> = budgetCache.toList()

    override suspend fun getAllFilteredBy(filter: BudgetFilter): List<Budget> = budgetCache.toList()

    fun setBudgets(budgets: List<Budget>) {
        budgetCache.clear()
        budgetCache.addAll(budgets)
    }
}
