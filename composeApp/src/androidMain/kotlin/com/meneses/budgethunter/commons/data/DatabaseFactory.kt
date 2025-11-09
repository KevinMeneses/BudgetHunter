package com.meneses.budgethunter.commons.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.meneses.budgethunter.db.Database

actual class DatabaseFactory(
    private val context: Context
) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = Database.Schema,
            context = context,
            name = "budgethunter_kmp.db"
        )
    }
}
