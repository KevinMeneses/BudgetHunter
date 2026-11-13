package com.meneses.budgethunter.commons.util

/**
 * Platform detection utility for KMP
 */
expect object Platform {
    val isAndroid: Boolean

    /**
     * Whether this binary was built for debugging (Android debug build type, iOS debug framework).
     * Gates every log that can print user data: never true in a published build.
     */
    val isDebugBuild: Boolean
}
