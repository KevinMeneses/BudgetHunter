package com.meneses.budgethunter.commons.data.network

import com.meneses.budgethunter.BuildKonfig

/**
 * Provides base URL for network requests from build configuration.
 *
 * The URL is configured in local.properties as BACKEND_URL.
 * - Android default: http://10.0.2.2:8080 (emulator localhost)
 * - iOS default: http://localhost:8080 (simulator shares host network)
 * - Production: Should be set in local.properties (e.g., https://api.budgethunter.com)
 */
fun getBaseUrl(): String = BuildKonfig.BACKEND_URL
