package com.meneses.budgethunter.splash.application

interface UpdateManager {
    fun verifyUpdate(context: Any, callback: (SplashState)-> Unit)
}

expect fun getUpdateManager(): UpdateManager


