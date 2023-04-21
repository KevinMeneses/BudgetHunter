package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.flow.onEach

class BudgetLocalRepository(
    private val budgetLocalDataSource: BudgetLocalDataSource = BudgetLocalDataSource()
) : BudgetRepository {
    override val budgetList
        get() = budgetLocalDataSource
            .budgetList
            .toDomain()
            .onEach { cachedList = it }

    override fun getAllBudgets() = cachedList

    override fun getBudgetsBy(budget: Budget) =
        cachedList.filter {
            if (budget.name.isBlank()) true
            else it.name.lowercase().contains(budget.name.lowercase())
        }.filter {
            if (budget.frequency == null) true
            else it.frequency == budget.frequency
        }

    override fun createBudget(budget: Budget): Budget {
        budgetLocalDataSource.insert(budget.toDb())
        val savedId = budgetLocalDataSource.selectLastId()
        return budget.copy(id = savedId)
    }

    override fun updateBudget(budget: Budget) =
        budgetLocalDataSource.update(budget.toDb())

    override fun deleteBudget(budget: Budget) =
        budgetLocalDataSource.delete(budget.id.toLong())

    companion object {
        private var cachedList = emptyList<Budget>()
    }
}