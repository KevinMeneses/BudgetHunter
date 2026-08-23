package com.meneses.budgethunter.di

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.splash.SplashScreenViewModel
import org.koin.dsl.module

val splashModule = module {
    factory<SplashScreenViewModel> {
        SplashScreenViewModel(
            appUpdateManager = get<AppUpdateManager>(),
            authRepository = get<AuthRepository>(),
            preferencesManager = get<PreferencesManager>()
        )
    }
}
