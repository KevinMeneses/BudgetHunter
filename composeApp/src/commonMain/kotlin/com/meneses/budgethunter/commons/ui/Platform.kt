package com.meneses.budgethunter.commons.ui

enum class Platform {
    ANDROID,
    IOS
}

expect fun getCurrentPlatform(): Platform