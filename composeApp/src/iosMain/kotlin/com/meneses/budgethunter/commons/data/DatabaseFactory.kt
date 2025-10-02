package com.meneses.budgethunter.commons.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.meneses.budgethunter.db.Database

actual class DatabaseFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = Database.Schema,
            name = "budgethunter_kmp.db"
        )
    }
}