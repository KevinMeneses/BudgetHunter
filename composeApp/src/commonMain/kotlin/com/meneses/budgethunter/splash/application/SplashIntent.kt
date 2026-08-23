package com.meneses.budgethunter.splash.application

sealed interface SplashIntent {
    data object VerifyUpdate : SplashIntent
}
