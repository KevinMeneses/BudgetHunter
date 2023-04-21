package com.meneses.budgethunter.budgetList.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.meneses.budgethunter.MyApplication
import com.meneses.budgethunter.db.Budget
import com.meneses.budgethunter.db.BudgetQueries
import com.meneses.budgethunter.db.Database
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.meneses.budgethunter.db.Budget as DbBudget

class BudgetLocalDataSource(
    driver: SqlDriver = MyApplication.driver,
    adapter: Budget.Adapter = DbBudget.Adapter(frequencyAdapter),
    database: Database = Database(driver, adapter),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val queries: BudgetQueries = database.budgetQueries

    val budgetList = queries
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
            frequency = budget.frequency,
        )

    fun delete(id: Long) = queries.delete(id)
}