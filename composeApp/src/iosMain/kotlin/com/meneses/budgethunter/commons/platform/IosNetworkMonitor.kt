package com.meneses.budgethunter.commons.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of NetworkMonitor using NWPathMonitor
 *
 * Uses Network framework's NWPathMonitor to track connectivity changes.
 * When offline, polls periodically to detect when network comes back online.
 */
@OptIn(ExperimentalForeignApi::class)
class IosNetworkMonitor : NetworkMonitor {

    private var pathMonitor = nw_path_monitor_create()
    private val monitorScope = CoroutineScope(Dispatchers.Main)
    private var pollingJob: Job? = null

    private val _isOnline = MutableStateFlow(false)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    override fun startMonitoring() {
        setupMonitor()
    }

    private fun setupMonitor() {
        nw_path_monitor_set_update_handler(pathMonitor) { path ->
            println("IosNetworkMonitor: Update handler called")
            path?.let {
                val status = nw_path_get_status(it)
                println("IosNetworkMonitor: Path status = $status")

                val isConnected = (status == nw_path_status_satisfied)
                println("IosNetworkMonitor: Setting isOnline to $isConnected")
                _isOnline.value = isConnected

                if (!isConnected) {
                    println("IosNetworkMonitor: Going offline, starting polling")
                    startPolling()
                } else {
                    println("IosNetworkMonitor: Going online, stopping polling")
                    stopPolling()
                }
            } ?: println("IosNetworkMonitor: Path is null")
        }
        nw_path_monitor_set_queue(pathMonitor, dispatch_get_main_queue())
        nw_path_monitor_start(pathMonitor)
        println("IosNetworkMonitor: Monitor started")
    }

    private fun startPolling() {
        stopPolling()
        pollingJob = monitorScope.launch {
            while (true) {
                delay(2000) // Poll every 2 seconds
                println("IosNetworkMonitor: Polling for network status")

                // Restart the monitor to get fresh status
                nw_path_monitor_cancel(pathMonitor)
                pathMonitor = nw_path_monitor_create()
                setupMonitor()

                // Break if we're back online
                if (_isOnline.value) {
                    println("IosNetworkMonitor: Detected online during polling")
                    break
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun stopMonitoring() {
        stopPolling()
        nw_path_monitor_cancel(pathMonitor)
        println("IosNetworkMonitor: Monitor stopped")
    }
}
