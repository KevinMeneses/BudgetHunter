package com.meneses.budgethunter

import android.app.Application
import com.meneses.budgethunter.di.androidPlatformModule
import com.meneses.budgethunter.di.initKoin
import com.meneses.budgethunter.sms.platform.BankNotificationListenerService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin with Android-specific modules
        initKoin(
            platformModule = module {
                includes(androidPlatformModule)
                single { this@MyApplication as Application }
                single { this@MyApplication }
            }
        ).apply {
            androidContext(this@MyApplication)
        }

        // The system may have dropped the bank notification listener and not bound it again
        BankNotificationListenerService.rebindIfGranted(this)
    }
}
