package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RequestSmsPermissionsIfNeededUseCaseTest {

    private val preferencesManager = mockk<PreferencesManager>()
    private val permissionsManager = mockk<PermissionsManager>(relaxed = true)
    private val useCase = RequestSmsPermissionsIfNeededUseCase(preferencesManager, permissionsManager)

    private fun givenPermissions(sms: Boolean, notifications: Boolean = true, rationale: Boolean = false) {
        every { permissionsManager.hasSmsPermission() } returns sms
        every { permissionsManager.hasNotificationPermission() } returns notifications
        every { permissionsManager.shouldShowSMSPermissionRationale() } returns rationale
    }

    @Test
    fun `requests the permissions when sms reading is restored without them`() = runTest {
        // Reinstall: off on the device until the account's value lands after sign in
        val smsReadingEnabled = MutableStateFlow(false)
        every { preferencesManager.smsReadingEnabled } returns smsReadingEnabled
        givenPermissions(sms = false)

        val job = launch { useCase.observe() }
        runCurrent()
        verify(exactly = 0) { permissionsManager.requestSmsPermissions(any()) }

        smsReadingEnabled.value = true
        runCurrent()
        verify(exactly = 1) { permissionsManager.requestSmsPermissions(any()) }
        job.cancel()
    }

    @Test
    fun `requests the permissions when only notifications are missing`() = runTest {
        every { preferencesManager.smsReadingEnabled } returns flowOf(true)
        givenPermissions(sms = true, notifications = false)

        useCase.observe()

        verify(exactly = 1) { permissionsManager.requestSmsPermissions(any()) }
    }

    @Test
    fun `does not request when sms reading is off`() = runTest {
        every { preferencesManager.smsReadingEnabled } returns flowOf(false)
        givenPermissions(sms = false)

        useCase.observe()

        verify(exactly = 0) { permissionsManager.requestSmsPermissions(any()) }
    }

    @Test
    fun `does not request when everything is granted`() = runTest {
        every { preferencesManager.smsReadingEnabled } returns flowOf(true)
        givenPermissions(sms = true, notifications = true)

        useCase.observe()

        verify(exactly = 0) { permissionsManager.requestSmsPermissions(any()) }
    }

    @Test
    fun `does not request once the user denied it`() = runTest {
        every { preferencesManager.smsReadingEnabled } returns flowOf(true)
        givenPermissions(sms = false, rationale = true)

        useCase.observe()

        verify(exactly = 0) { permissionsManager.requestSmsPermissions(any()) }
    }

    @Test
    fun `requests at most once per process`() = runTest {
        every { preferencesManager.smsReadingEnabled } returns flowOf(true)
        givenPermissions(sms = false)

        useCase.observe()
        useCase.observe()

        verify(exactly = 1) { permissionsManager.requestSmsPermissions(any()) }
    }
}
