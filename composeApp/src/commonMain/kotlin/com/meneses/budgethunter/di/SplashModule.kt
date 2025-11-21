package com.meneses.budgethunter.di

import com.meneses.budgethunter.commons.platform.IAppUpdateManager
import com.meneses.budgethunter.splash.SplashScreenViewModel
import org.koin.dsl.module

val splashModule = module {
    factory<SplashScreenViewModel> {
        SplashScreenViewModel(get<IAppUpdateManager>())
    }
}
