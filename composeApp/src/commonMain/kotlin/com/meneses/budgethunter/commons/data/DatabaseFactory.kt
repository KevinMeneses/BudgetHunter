package com.meneses.budgethunter.commons.data

import com.meneses.budgethunter.db.Database

expect class DatabaseFactory {
    fun createDatabase(): Database
}