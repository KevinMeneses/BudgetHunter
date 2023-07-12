package com.meneses.budgethunter.splash.application

import android.content.Context

sealed interface SplashEvent {
    data class VerifyUpdate(val context: Context) : SplashEvent
}
