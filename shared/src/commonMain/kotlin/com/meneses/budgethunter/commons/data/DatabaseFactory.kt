package com.meneses.budgethunter.commons.data

import com.meneses.budgethunter.db.Database

interface DatabaseFactory {
    fun create()

    companion object {
        lateinit var database: Database
    }
}

expect fun initDatabaseFactory(context: Any)
