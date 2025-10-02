package com.meneses.budgethunter.commons.data

import app.cash.sqldelight.db.SqlDriver
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database

expect class DatabaseFactory {
    fun createDriver(): SqlDriver
}

fun DatabaseFactory.createDatabase(): Database {
    val budgetEntryAdapter = Budget_entry.Adapter(typeAdapter, categoryAdapter)
    val driver = createDriver()
    return Database(driver, budgetEntryAdapter)
}
