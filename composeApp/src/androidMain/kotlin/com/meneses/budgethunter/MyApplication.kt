package com.meneses.budgethunter

import android.app.Application
import com.meneses.budgethunter.di.androidPlatformModule
import com.meneses.budgethunter.di.initKoin
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
    }
}
