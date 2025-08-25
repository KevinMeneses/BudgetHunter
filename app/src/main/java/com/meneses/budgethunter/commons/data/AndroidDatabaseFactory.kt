package com.meneses.budgethunter.commons.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database

class AndroidDatabaseFactory(
    private val context: Context
) : DatabaseFactory {
    override fun createDatabase(): Database {
        val budgetEntryAdapter = Budget_entry.Adapter(typeAdapter, categoryAdapter)
        val driver = createDriver()
        return Database(driver, budgetEntryAdapter)
    }

    private fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = Database.Schema,
            context = context,
            name = "budgethunter.db"
        )
    }
}
