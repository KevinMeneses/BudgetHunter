package com.meneses.budgethunter.commons.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database

actual class DatabaseFactory {
    actual fun createDatabase(): Database {
        val budgetEntryAdapter = Budget_entry.Adapter(typeAdapter, categoryAdapter)
        val driver = createDriver()
        return Database(driver, budgetEntryAdapter)
    }

    private fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = Database.Schema,
            name = "budgethunter_kmp.db"
        )
    }
}