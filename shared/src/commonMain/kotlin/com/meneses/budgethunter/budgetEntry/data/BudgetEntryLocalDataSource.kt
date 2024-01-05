package com.meneses.budgethunter.budgetEntry.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.commons.data.DatabaseFactory
import com.meneses.budgethunter.db.BudgetEntryQueries
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class BudgetEntryLocalDataSource(
    private val queries: BudgetEntryQueries = DatabaseFactory.database.budgetEntryQueries,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun selectAllByBudgetId(budgetId: Long) = queries
        .selectAllByBudgetId(budgetId)
        .asFlow()
        .mapToList(dispatcher)

    fun insert(budgetEntry: Budget_entry) = queries
        .insert(
            id = null,
            budgetId = budgetEntry.budget_id,
            amount = budgetEntry.amount,
            description = budgetEntry.description,
            type = budgetEntry.type,
            date = budgetEntry.date
        )

    fun update(budgetEntry: Budget_entry) = queries
        .update(
            id = budgetEntry.id,
            budgetId = budgetEntry.budget_id,
            amount = budgetEntry.amount,
            description = budgetEntry.description,
            type = budgetEntry.type,
            date = budgetEntry.date
        )

    fun deleteByIds(list: List<Long>) =
        queries.deleteByIds(list)
}
