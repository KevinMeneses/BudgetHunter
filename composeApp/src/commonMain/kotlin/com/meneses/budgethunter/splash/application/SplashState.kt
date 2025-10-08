package com.meneses.budgethunter.splash.application

data class SplashState(
    val navigate: Boolean = false,
    val updatingApp: Boolean = false,
    val isAuthenticated: Boolean = false
)