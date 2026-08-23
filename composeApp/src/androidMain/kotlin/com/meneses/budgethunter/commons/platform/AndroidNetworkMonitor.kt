package com.meneses.budgethunter.commons.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val OFFLINE_DEBOUNCE_MS = 500L

/**
 * Android implementation of NetworkMonitor using ConnectivityManager
 *
 * Uses NetworkCallback to monitor connectivity changes in real-time.
 * Checks for actual internet capability (not just network connection).
 *
 * A short debounce is applied before emitting the offline state to avoid false
 * negatives during the brief window where NET_CAPABILITY_VALIDATED is not yet
 * set (e.g. metered or captive-portal networks). Going online is emitted immediately.
 */
class AndroidNetworkMonitor(
    context: Context,
    private val scope: CoroutineScope
) : NetworkMonitor {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var offlineDebounceJob: Job? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            scheduleOffline()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            // This is called when network capabilities change, including when validation completes
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val hasTransport = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

            val online = hasInternet && isValidated && hasTransport
            if (online) {
                // Cancel any pending offline transition and go online immediately
                offlineDebounceJob?.cancel()
                offlineDebounceJob = null
                _isOnline.value = true
            } else {
                scheduleOffline()
            }
        }
    }

    override fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Re-check connectivity after registering callback to ensure accurate initial state
        _isOnline.value = checkCurrentConnectivity()
    }

    override fun stopMonitoring() {
        offlineDebounceJob?.cancel()
        offlineDebounceJob = null
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: IllegalArgumentException) {
            // Callback was not registered, ignore
        }
    }

    /**
     * Schedules an offline state transition after [OFFLINE_DEBOUNCE_MS].
     * Cancels any previously scheduled transition so rapid connectivity
     * changes don't flip the state unnecessarily.
     */
    private fun scheduleOffline() {
        offlineDebounceJob?.cancel()
        offlineDebounceJob = scope.launch {
            delay(OFFLINE_DEBOUNCE_MS)
            _isOnline.value = false
        }
    }

    /**
     * Check current connectivity state synchronously
     * Returns true only if there's an active network with validated internet connectivity
     */
    private fun checkCurrentConnectivity(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val hasTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        return hasInternet && isValidated && hasTransport
    }
}
