package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.flow.Flow

class BudgetLocalRepository(
    private val budgetLocalDataSource: BudgetLocalDataSource = BudgetLocalDataSource()
) : BudgetRepository {

    override val budgetList: Flow<List<Budget>> get() = _budgetList
    private var _budgetList = budgetLocalDataSource.budgetList.toDomain()

    override fun getAllBudgets() {
        _budgetList = budgetLocalDataSource.budgetList.toDomain()
    }

    override fun getBudgetsBy(budget: Budget) {
        val dbBudget = budget.toDb()
        _budgetList = budgetLocalDataSource
            .selectAllBy(dbBudget)
            .toDomain()
    }

    override fun createBudget(budget: Budget): Budget {
        val dbBudget = budget.toDb()
        budgetLocalDataSource.insert(dbBudget)
        val savedId = budgetLocalDataSource.selectLastId()
        return budget.copy(id = savedId)
    }

    override fun updateBudget(budget: Budget) {
        val dbBudget = budget.toDb()
        budgetLocalDataSource.update(dbBudget)
    }

    override fun deleteBudget(budget: Budget) {
        budgetLocalDataSource.delete(budget.id.toLong())
    }
}