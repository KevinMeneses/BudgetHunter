package com.meneses.budgethunter.splash.di

import com.meneses.budgethunter.splash.SplashScreenViewModel
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
class SplashModule {

    @Factory
    fun provideSplashScreenViewModel(): SplashScreenViewModel = SplashScreenViewModel()
}
