package com.meneses.budgethunter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.meneses.budgethunter.budgetDetail.di.BudgetDetailModule
import com.meneses.budgethunter.budgetEntry.di.BudgetEntryModule
import com.meneses.budgethunter.budgetList.di.BudgetListModule
import com.meneses.budgethunter.budgetMetrics.di.BudgetMetricsModule
import com.meneses.budgethunter.di.AppModule
import com.meneses.budgethunter.settings.di.SettingsModule
import com.meneses.budgethunter.sms.di.SmsModule
import com.meneses.budgethunter.splash.di.SplashModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(
                AppModule().module,
                BudgetListModule().module,
                BudgetDetailModule().module,
                BudgetEntryModule().module,
                BudgetMetricsModule().module,
                SettingsModule().module,
                SplashModule().module,
                SmsModule().module
            )
        }

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
        private const val SMS_TRANSACTION_CHANNEL_ID = "sms_transactions"
    }
}
