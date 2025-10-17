package com.meneses.budgethunter.commons.data.network

/**
 * Provides platform-specific base URL for network requests.
 *
 * - Android emulator: Uses 10.0.2.2 to access host machine
 * - iOS simulator: Uses localhost directly (shares network with host)
 */
expect fun getBaseUrl(): String