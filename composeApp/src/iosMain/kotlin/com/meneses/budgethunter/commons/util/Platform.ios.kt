package com.meneses.budgethunter.commons.util

import kotlin.experimental.ExperimentalNativeApi

actual object Platform {
    actual val isAndroid: Boolean = false

    // True only for a debug framework: Xcode's Release configuration links the release one
    @OptIn(ExperimentalNativeApi::class)
    actual val isDebugBuild: Boolean = kotlin.native.Platform.isDebugBinary
}
