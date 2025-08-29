package com.meneses.budgethunter.splash.di

import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.splash.SplashScreenViewModel
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
class SplashModule {

    @Factory
    fun provideSplashScreenViewModel(
        appUpdateManager: AppUpdateManager
    ): SplashScreenViewModel = SplashScreenViewModel(appUpdateManager)
}
