package com.meneses.budgethunter.budgetList.data.repository

import com.meneses.budgethunter.budgetList.data.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.toDb
import com.meneses.budgethunter.budgetList.data.toDomain
import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.flow.onEach

class BudgetLocalRepository(
    private val budgetLocalDataSource: BudgetLocalDataSource = BudgetLocalDataSource()
) : BudgetRepository {
    override val budgets
        get() = budgetLocalDataSource
            .budgets
            .toDomain()
            .onEach { cachedList = it }

    override fun getAll() = cachedList

    override fun getAllFilteredBy(budget: Budget) =
        cachedList.filter {
            if (budget.name.isBlank()) true
            else it.name.lowercase().contains(budget.name.lowercase())
        }.filter {
            if (budget.frequency == null) true
            else it.frequency == budget.frequency
        }

    override fun create(budget: Budget): Budget {
        budgetLocalDataSource.insert(budget.toDb())
        val savedId = budgetLocalDataSource.selectLastId()
        return budget.copy(id = savedId)
    }

    override fun update(budget: Budget) =
        budgetLocalDataSource.update(budget.toDb())

    override fun delete(budget: Budget) =
        budgetLocalDataSource.delete(budget.id.toLong())

    companion object {
        private var cachedList = emptyList<Budget>()
    }
}