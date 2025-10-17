package com.meneses.budgethunter.commons.data.network

/**
 * iOS implementation of base URL.
 * iOS Simulator shares the same network as the host machine.
 */
actual fun getBaseUrl(): String = "http://localhost:8080"