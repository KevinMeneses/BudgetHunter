package com.meneses.budgethunter.commons.util

import com.meneses.budgethunter.BuildConfig

actual object Platform {
    actual val isAndroid: Boolean = true

    // Android's own BuildConfig follows the build type (debug / release) of the APK or AAB
    actual val isDebugBuild: Boolean = BuildConfig.DEBUG
}
