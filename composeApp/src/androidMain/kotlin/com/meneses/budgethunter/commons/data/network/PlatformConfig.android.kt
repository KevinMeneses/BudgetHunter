package com.meneses.budgethunter.commons.data.network

/**
 * Android implementation of base URL.
 * Uses 10.0.2.2 to access host machine from Android emulator.
 */
actual fun getBaseUrl(): String = "http://10.0.2.2:8080"