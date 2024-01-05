package com.meneses.budgethunter.budgetList.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.commons.data.DatabaseFactory
import com.meneses.budgethunter.db.BudgetQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.meneses.budgethunter.db.Budget as DbBudget

class BudgetLocalDataSource(
    private val queries: BudgetQueries = DatabaseFactory.database.budgetQueries,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val budgets = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)

    fun insert(budget: DbBudget) = queries
        .insert(
            id = null,
            name = budget.name,
            amount = budget.amount,
            frequency = budget.frequency
        )

    fun selectLastId() = queries
        .selectLastId()
        .executeAsOne()
        .toInt()

    fun update(budget: DbBudget) = queries
        .update(
            id = budget.id,
            amount = budget.amount,
            name = budget.name,
            frequency = budget.frequency
        )

    fun delete(id: Long) = queries.delete(id)
}
