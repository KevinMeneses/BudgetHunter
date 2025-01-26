package com.meneses.budgethunter.commons.data

import app.cash.sqldelight.db.SqlDriver
import com.meneses.budgethunter.MyApplication
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database

class AndroidDatabaseFactory(
    private val driver: SqlDriver = MyApplication.driver,
    private val budgetEntryAdapter: Budget_entry.Adapter = Budget_entry.Adapter(typeAdapter, categoryAdapter)
) : DatabaseFactory {
    override fun create() = Database(driver, budgetEntryAdapter)
}
