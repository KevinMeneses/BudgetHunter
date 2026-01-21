package com.meneses.budgethunter.commons.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific network connectivity monitor
 *
 * Monitors network connectivity state and exposes it as a reactive StateFlow.
 * This allows the app to respond to connectivity changes and adjust sync behavior accordingly.
 *
 * Platform implementations:
 * - Android: Uses ConnectivityManager.NetworkCallback
 * - iOS: Uses NWPathMonitor from Network framework
 */
interface NetworkMonitor {
    /**
     * Current network connectivity state
     * - true: Device has active internet connection
     * - false: Device is offline (no network or no internet access)
     */
    val isOnline: StateFlow<Boolean>

    /**
     * Start monitoring network connectivity changes
     * Should be called when the monitor is first needed
     */
    fun startMonitoring()

    /**
     * Stop monitoring network connectivity changes
     * Should be called when the monitor is no longer needed to release resources
     */
    fun stopMonitoring()
}
