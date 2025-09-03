package com.meneses.budgethunter.di

import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.splash.SplashScreenViewModel
import org.koin.dsl.module

val splashModule = module {
    single<SplashScreenViewModel> {
        SplashScreenViewModel(get<AppUpdateManager>())
    }
}