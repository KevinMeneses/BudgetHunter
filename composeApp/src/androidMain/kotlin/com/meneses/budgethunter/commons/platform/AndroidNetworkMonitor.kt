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
 * A device usually has several networks at once (e.g. WiFi and cellular) and the callbacks
 * report on each one separately, so the monitor keeps the set of networks that currently
 * have validated internet and is online while that set is not empty. Losing one network, or
 * one network reporting it is not validated yet, must not mark the device offline while
 * another network still works.
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

    // Callbacks arrive on the ConnectivityManager thread; the lock also guards offlineDebounceJob.
    private val lock = Any()
    private val validatedNetworks = mutableSetOf<Network>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) = synchronized(lock) {
            validatedNetworks.remove(network)
            updateOnlineState()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) = synchronized(lock) {
            // This is called when network capabilities change, including when validation completes
            if (networkCapabilities.hasValidatedInternet()) {
                validatedNetworks.add(network)
            } else {
                validatedNetworks.remove(network)
            }
            updateOnlineState()
        }
    }

    private fun updateOnlineState() {
        if (validatedNetworks.isNotEmpty()) {
            // Cancel any pending offline transition and go online immediately
            offlineDebounceJob?.cancel()
            offlineDebounceJob = null
            _isOnline.value = true
        } else {
            scheduleOffline()
        }
    }

    override fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // The initial state comes from the constructor; registering replays the current networks
        // through the callback, which keeps validatedNetworks and isOnline consistent. Re-reading
        // activeNetwork here would race with those callbacks and could overwrite them.
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun stopMonitoring() {
        synchronized(lock) {
            offlineDebounceJob?.cancel()
            offlineDebounceJob = null
            validatedNetworks.clear()
        }
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: IllegalArgumentException) {
            // Callback was not registered, ignore
        }
    }

    /**
     * Schedules an offline state transition after [OFFLINE_DEBOUNCE_MS], unless one is already
     * pending. A network that becomes validated in the meantime cancels it, so rapid
     * connectivity changes don't flip the state unnecessarily.
     */
    private fun scheduleOffline() {
        if (offlineDebounceJob?.isActive == true) return
        offlineDebounceJob = scope.launch {
            delay(OFFLINE_DEBOUNCE_MS)
            synchronized(lock) {
                if (validatedNetworks.isEmpty()) _isOnline.value = false
            }
        }
    }

    /**
     * Check current connectivity state synchronously
     * Returns true only if there's an active network with validated internet connectivity
     */
    private fun checkCurrentConnectivity(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasValidatedInternet()
    }

    private fun NetworkCapabilities.hasValidatedInternet(): Boolean {
        val hasInternet = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val hasTransport = hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        return hasInternet && isValidated && hasTransport
    }
}
