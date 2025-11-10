package com.meneses.budgethunter.splash.application

sealed interface SplashEvent {
    data object VerifyUpdate : SplashEvent
}
