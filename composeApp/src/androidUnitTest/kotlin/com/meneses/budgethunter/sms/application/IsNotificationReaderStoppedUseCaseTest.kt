package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsNotificationReaderStoppedUseCaseTest {

    private val preferencesManager = mockk<PreferencesManager>()
    private val permissionsManager = mockk<PermissionsManager>()
    private val isNotificationReaderStopped = IsNotificationReaderStoppedUseCase(preferencesManager, permissionsManager)

    @BeforeTest
    fun setup() {
        coEvery { preferencesManager.isBankNotificationReadingEnabled() } returns true
        coEvery { preferencesManager.isBankEmailReadingEnabled() } returns false
        every { permissionsManager.hasNotificationListenerAccess() } returns true
        every { permissionsManager.areAppNotificationsEnabled() } returns true
        every { permissionsManager.isNotificationListenerConnected() } returns false
    }

    @Test
    fun `stopped when everything is granted and the reader stays unbound`() = runTest {
        assertTrue(isNotificationReaderStopped())
    }

    @Test
    fun `not stopped when the reader connects within the grace period`() = runTest {
        every { permissionsManager.isNotificationListenerConnected() } returnsMany listOf(false, true)

        assertFalse(isNotificationReaderStopped())
    }

    @Test
    fun `not stopped while connected`() = runTest {
        every { permissionsManager.isNotificationListenerConnected() } returns true

        assertFalse(isNotificationReaderStopped())
    }

    @Test
    fun `not stopped with both switches off`() = runTest {
        coEvery { preferencesManager.isBankNotificationReadingEnabled() } returns false

        assertFalse(isNotificationReaderStopped())
    }

    @Test
    fun `reading bank emails alone also needs the reader`() = runTest {
        coEvery { preferencesManager.isBankNotificationReadingEnabled() } returns false
        coEvery { preferencesManager.isBankEmailReadingEnabled() } returns true

        assertTrue(isNotificationReaderStopped())
    }

    @Test
    fun `missing access or app notifications are warned about elsewhere, not as a stop`() = runTest {
        every { permissionsManager.hasNotificationListenerAccess() } returns false
        assertFalse(isNotificationReaderStopped())

        every { permissionsManager.hasNotificationListenerAccess() } returns true
        every { permissionsManager.areAppNotificationsEnabled() } returns false
        assertFalse(isNotificationReaderStopped())
    }
}
