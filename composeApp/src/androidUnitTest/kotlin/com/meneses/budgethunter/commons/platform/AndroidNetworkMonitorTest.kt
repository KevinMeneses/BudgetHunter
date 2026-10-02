package com.meneses.budgethunter.commons.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [AndroidNetworkMonitor]. Robolectric provides the real NetworkRequest.Builder
 * and a usable NetworkCallback base class; Context and ConnectivityManager are mocked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class AndroidNetworkMonitorTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifi: Network
    private lateinit var cellular: Network
    private val callbackSlot = slot<ConnectivityManager.NetworkCallback>()

    private val validatedWifiCaps = caps(validated = true, transport = NetworkCapabilities.TRANSPORT_WIFI)
    private val validatedCellularCaps = caps(validated = true, transport = NetworkCapabilities.TRANSPORT_CELLULAR)
    private val unvalidatedCellularCaps = caps(validated = false, transport = NetworkCapabilities.TRANSPORT_CELLULAR)

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        wifi = mockk(relaxed = true)
        cellular = mockk(relaxed = true)
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns null
        every {
            connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot))
        } returns Unit
    }

    private fun caps(validated: Boolean, transport: Int, internet: Boolean = true): NetworkCapabilities {
        val c = mockk<NetworkCapabilities>()
        every { c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns internet
        every { c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns validated
        every { c.hasTransport(any()) } answers { firstArg<Int>() == transport }
        return c
    }

    private fun TestScope.startMonitor(): AndroidNetworkMonitor {
        val monitor = AndroidNetworkMonitor(context, this)
        monitor.startMonitoring()
        verify { connectivityManager.registerNetworkCallback(any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>()) }
        return monitor
    }

    private val callback get() = callbackSlot.captured

    @Test
    fun `initial state is offline when activeNetwork is null`() = runTest {
        val monitor = startMonitor()
        runCurrent()
        assertFalse(monitor.isOnline.value)
    }

    @Test
    fun `stays online when one of two validated networks is lost`() = runTest {
        val monitor = startMonitor()
        callback.onCapabilitiesChanged(wifi, validatedWifiCaps)
        callback.onCapabilitiesChanged(cellular, validatedCellularCaps)
        assertTrue(monitor.isOnline.value)

        callback.onLost(cellular)
        advanceTimeBy(600)
        runCurrent()

        assertTrue("wifi is still validated, should remain online", monitor.isOnline.value)
        advanceUntilIdle()
        assertTrue(monitor.isOnline.value)
    }

    @Test
    fun `stays online when another network reports capabilities without VALIDATED`() = runTest {
        val monitor = startMonitor()
        callback.onCapabilitiesChanged(wifi, validatedWifiCaps)
        assertTrue(monitor.isOnline.value)

        callback.onCapabilitiesChanged(cellular, unvalidatedCellularCaps)
        advanceTimeBy(600)
        runCurrent()

        assertTrue("wifi is still validated, should remain online", monitor.isOnline.value)
        advanceUntilIdle()
        assertTrue(monitor.isOnline.value)
    }

    @Test
    fun `goes offline only after 500ms when last validated network is lost`() = runTest {
        val monitor = startMonitor()
        callback.onCapabilitiesChanged(wifi, validatedWifiCaps)
        assertTrue(monitor.isOnline.value)

        callback.onLost(wifi)
        advanceTimeBy(499)
        runCurrent()
        assertTrue("must not be offline before debounce elapses", monitor.isOnline.value)

        advanceTimeBy(2)
        runCurrent()
        assertFalse(monitor.isOnline.value)
    }

    @Test
    fun `further unvalidated updates do not postpone a pending offline transition`() = runTest {
        val monitor = startMonitor()
        callback.onCapabilitiesChanged(wifi, validatedWifiCaps)

        callback.onLost(wifi)
        advanceTimeBy(400)
        callback.onCapabilitiesChanged(cellular, unvalidatedCellularCaps)
        advanceTimeBy(101)
        runCurrent()

        assertFalse("offline must happen 500ms after the last validated network was lost", monitor.isOnline.value)
    }

    @Test
    fun `goes online immediately when a network becomes validated while offline`() = runTest {
        val monitor = startMonitor()
        assertFalse(monitor.isOnline.value)

        callback.onCapabilitiesChanged(wifi, validatedWifiCaps)

        assertTrue("going online must not wait for the scheduler", monitor.isOnline.value)
    }

    @Test
    fun `pending offline is cancelled when another network becomes validated within debounce`() = runTest {
        val monitor = startMonitor()
        callback.onCapabilitiesChanged(wifi, validatedWifiCaps)

        callback.onLost(wifi)
        advanceTimeBy(200)
        runCurrent()
        callback.onCapabilitiesChanged(cellular, validatedCellularCaps)
        advanceTimeBy(1000)
        runCurrent()
        advanceUntilIdle()

        assertTrue(monitor.isOnline.value)
    }

    @Test
    fun `emulator sequence toggling wifi and data ends online`() = runTest {
        val monitor = startMonitor()
        callback.onCapabilitiesChanged(wifi, validatedWifiCaps)
        callback.onCapabilitiesChanged(cellular, validatedCellularCaps)
        assertTrue(monitor.isOnline.value)

        callback.onLost(wifi)
        callback.onLost(cellular)
        advanceTimeBy(600)
        runCurrent()
        assertFalse("everything lost, should be offline", monitor.isOnline.value)

        callback.onCapabilitiesChanged(wifi, validatedWifiCaps)
        assertTrue(monitor.isOnline.value)

        callback.onCapabilitiesChanged(cellular, unvalidatedCellularCaps)
        callback.onLost(cellular)
        advanceTimeBy(600)
        runCurrent()
        advanceUntilIdle()

        assertTrue("wifi is validated, must end online", monitor.isOnline.value)
    }
}
