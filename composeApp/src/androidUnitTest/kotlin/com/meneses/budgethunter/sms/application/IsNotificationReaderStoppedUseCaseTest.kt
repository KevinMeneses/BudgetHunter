package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.sms.domain.ReadingChannel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IsNotificationReaderStoppedUseCaseTest {

    private val preferencesManager = mockk<PreferencesManager>()
    private val permissionsManager = mockk<PermissionsManager>()

    // The use case counts its grace period on the test's virtual clock
    private fun TestScope.useCase() =
        IsNotificationReaderStoppedUseCase(preferencesManager, permissionsManager, testScheduler.timeSource)

    @BeforeTest
    fun setup() {
        coEvery { preferencesManager.getChannelsInUse() } returns setOf(ReadingChannel.APP)
        every { permissionsManager.hasNotificationListenerAccess() } returns true
        every { permissionsManager.areAppNotificationsEnabled() } returns true
        every { permissionsManager.isNotificationListenerConnected() } returns false
    }

    @Test
    fun `stopped when everything is granted and the reader stays unbound`() = runTest {
        val isNotificationReaderStopped = useCase()
        assertTrue(isNotificationReaderStopped())
    }

    @Test
    fun `not stopped when the reader connects within the grace period`() = runTest {
        val isNotificationReaderStopped = useCase()
        every { permissionsManager.isNotificationListenerConnected() } returnsMany listOf(false, true)

        assertFalse(isNotificationReaderStopped())
    }

    @Test
    fun `not stopped while connected`() = runTest {
        val isNotificationReaderStopped = useCase()
        every { permissionsManager.isNotificationListenerConnected() } returns true

        assertFalse(isNotificationReaderStopped())
    }

    @Test
    fun `not stopped while no bank is read from an app or by email`() = runTest {
        val isNotificationReaderStopped = useCase()
        coEvery { preferencesManager.getChannelsInUse() } returns setOf(ReadingChannel.SMS)

        assertFalse(isNotificationReaderStopped())
    }

    @Test
    fun `reading bank emails alone also needs the reader`() = runTest {
        val isNotificationReaderStopped = useCase()
        coEvery { preferencesManager.getChannelsInUse() } returns setOf(ReadingChannel.EMAIL)

        assertTrue(isNotificationReaderStopped())
    }

    @Test
    fun `missing access or app notifications are warned about elsewhere, not as a stop`() = runTest {
        val isNotificationReaderStopped = useCase()
        every { permissionsManager.hasNotificationListenerAccess() } returns false
        assertFalse(isNotificationReaderStopped())

        every { permissionsManager.hasNotificationListenerAccess() } returns true
        every { permissionsManager.areAppNotificationsEnabled() } returns false
        assertFalse(isNotificationReaderStopped())
    }

    @Test
    fun `waits the grace period on the first check`() = runTest {
        val isNotificationReaderStopped = useCase()

        val check = async { isNotificationReaderStopped() }
        advanceTimeBy(2_999)
        runCurrent()
        assertFalse(check.isCompleted)

        advanceTimeBy(1)
        runCurrent()
        assertTrue(check.await())
    }

    @Test
    fun `answers at once once the grace period is over`() = runTest {
        val isNotificationReaderStopped = useCase()
        assertTrue(isNotificationReaderStopped())

        val start = testScheduler.currentTime
        assertTrue(isNotificationReaderStopped())
        assertEquals(start, testScheduler.currentTime)
    }

    @Test
    fun `granting the access again starts a new grace period`() = runTest {
        val isNotificationReaderStopped = useCase()
        assertTrue(isNotificationReaderStopped())

        every { permissionsManager.hasNotificationListenerAccess() } returns false
        assertFalse(isNotificationReaderStopped())

        every { permissionsManager.hasNotificationListenerAccess() } returns true
        val start = testScheduler.currentTime
        assertTrue(isNotificationReaderStopped())
        assertEquals(start + 3_000, testScheduler.currentTime)
    }

    @Test
    fun `a connected reader is answered without waiting`() = runTest {
        val isNotificationReaderStopped = useCase()
        every { permissionsManager.isNotificationListenerConnected() } returns true

        val start = testScheduler.currentTime
        assertFalse(isNotificationReaderStopped())
        assertEquals(start, testScheduler.currentTime)
        verify(exactly = 1) { permissionsManager.isNotificationListenerConnected() }
    }
}
