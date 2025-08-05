package com.meneses.budgethunter

import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.db.Database

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        driver = AndroidSqliteDriver(Database.Schema, applicationContext, "budgethunter.db")
        preferencesManager = PreferencesManager(applicationContext)
    }

    companion object {
        lateinit var driver: AndroidSqliteDriver
        lateinit var preferencesManager: PreferencesManager
    }
}
