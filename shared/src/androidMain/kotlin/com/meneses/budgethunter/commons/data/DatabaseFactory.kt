package com.meneses.budgethunter.commons.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.meneses.budgethunter.budgetList.data.adapter.frequencyAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.commons.data.DatabaseFactory.Companion.database
import com.meneses.budgethunter.db.Budget
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database

class AndroidDatabaseFactory(private val context: Context) : DatabaseFactory {
    override fun create() {
        val driver = AndroidSqliteDriver(Database.Schema, context, "budgethunter.db")
        val budgetAdapter = Budget.Adapter(frequencyAdapter)
        val budgetEntryAdapter = Budget_entry.Adapter(typeAdapter)
        database = Database(driver, budgetAdapter, budgetEntryAdapter)
    }
}

actual fun initDatabaseFactory(context: Any): Unit =
    AndroidDatabaseFactory(context as Context).create()
