package com.meneses.budgethunter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.db.Database

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        driver = AndroidSqliteDriver(Database.Schema, applicationContext, "budgethunter.db")
        preferencesManager = PreferencesManager(applicationContext)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nameSms = getString(R.string.channel_name_sms_transactions)
        val descriptionTextSms = getString(R.string.channel_description_sms_transactions)
        val importanceSms = NotificationManager.IMPORTANCE_HIGH
        val channelSms = NotificationChannel(SMS_TRANSACTION_CHANNEL_ID, nameSms, importanceSms).apply {
            description = descriptionTextSms
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channelSms)
    }

    companion object {
        lateinit var driver: AndroidSqliteDriver
        lateinit var preferencesManager: PreferencesManager

        private const val SMS_TRANSACTION_CHANNEL_ID = "sms_transactions"
    }
}
