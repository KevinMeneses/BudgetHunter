package com.meneses.budgethunter.commons.data

import com.meneses.budgethunter.db.Database

interface DatabaseFactory {
    fun createDatabase(): Database
}
