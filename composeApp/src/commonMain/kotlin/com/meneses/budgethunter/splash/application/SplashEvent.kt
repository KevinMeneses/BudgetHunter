package com.meneses.budgethunter.splash.application

sealed interface SplashEvent {
    data object NavigateToSignIn : SplashEvent
    data object NavigateToBudgetList : SplashEvent
}
