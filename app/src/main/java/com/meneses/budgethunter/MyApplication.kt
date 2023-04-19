package com.meneses.budgethunter

import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.meneses.budgethunter.db.Database

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        driver = AndroidSqliteDriver(Database.Schema, applicationContext, "budgethunter.db")
    }

    companion object {
        lateinit var driver: AndroidSqliteDriver
    }
}