package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WarnIfNotificationReaderStoppedUseCaseTest {

    private val isNotificationReaderStopped = mockk<IsNotificationReaderStoppedUseCase>()
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val notificationManager = mockk<NotificationManager>(relaxed = true)
    private val stringResourceProvider = mockk<StringResourceProvider>()

    private val warnIfStopped = WarnIfNotificationReaderStoppedUseCase(
        isNotificationReaderStopped,
        preferencesManager,
        notificationManager,
        stringResourceProvider
    )

    @BeforeTest
    fun setup() {
        coEvery { stringResourceProvider.getString(any()) } returns "text"
        coEvery { preferencesManager.isReaderStoppedNotified() } returns false
    }

    @Test
    fun `a reader that is running is not warned about`() = runTest {
        coEvery { isNotificationReaderStopped() } returns false

        warnIfStopped()

        verify(exactly = 0) { notificationManager.showReaderStoppedNotification(any(), any()) }
    }

    @Test
    fun `a reader that connects again within the confirmation is not warned about`() = runTest {
        coEvery { isNotificationReaderStopped() } returnsMany listOf(true, false)

        warnIfStopped()

        verify(exactly = 0) { notificationManager.showReaderStoppedNotification(any(), any()) }
    }

    @Test
    fun `a reader still stopped after the confirmation is warned about once with a way to Settings`() = runTest {
        coEvery { isNotificationReaderStopped() } returns true

        warnIfStopped()

        verify(exactly = 1) { notificationManager.showReaderStoppedNotification(any(), any()) }
        coVerify { preferencesManager.setReaderStoppedNotified(true) }
    }

    @Test
    fun `a stop already warned about is not warned again`() = runTest {
        coEvery { isNotificationReaderStopped() } returns true
        coEvery { preferencesManager.isReaderStoppedNotified() } returns true

        warnIfStopped()

        verify(exactly = 0) { notificationManager.showReaderStoppedNotification(any(), any()) }
        coVerify(exactly = 0) { preferencesManager.setReaderStoppedNotified(any()) }
    }

    @Test
    fun `a stopped reader is checked again only after the confirmation wait`() = runTest {
        coEvery { isNotificationReaderStopped() } returns true

        warnIfStopped()

        assertEquals(60_000L, testScheduler.currentTime)
        coVerify(exactly = 2) { isNotificationReaderStopped() }
    }

    @Test
    fun `a reader running again lets the next stop be warned about`() = runTest {
        coEvery { isNotificationReaderStopped() } returns false
        coEvery { preferencesManager.isReaderStoppedNotified() } returns true

        warnIfStopped()

        coVerify { preferencesManager.setReaderStoppedNotified(false) }
        verify { notificationManager.cancelReaderStoppedNotification() }
    }

    @Test
    fun `a reader running with no warning given cancels nothing`() = runTest {
        coEvery { isNotificationReaderStopped() } returns false

        warnIfStopped()

        verify(exactly = 0) { notificationManager.cancelReaderStoppedNotification() }
    }

    @Test
    fun `a stop is warned about with the reader's own notification`() = runTest {
        coEvery { isNotificationReaderStopped() } returns true

        warnIfStopped()

        verify(exactly = 0) { notificationManager.showOpenSettingsNotification(any(), any()) }
    }

    @Test
    fun `a stop that cannot be shown is neither shown nor marked as told`() = runTest {
        coEvery { isNotificationReaderStopped() } returns true

        warnIfStopped(canNotify = false)

        verify(exactly = 0) { notificationManager.showReaderStoppedNotification(any(), any()) }
        coVerify(exactly = 0) { preferencesManager.setReaderStoppedNotified(any()) }
    }

    @Test
    fun `a reader running again clears the mark even while warnings cannot be shown`() = runTest {
        coEvery { isNotificationReaderStopped() } returns false
        coEvery { preferencesManager.isReaderStoppedNotified() } returns true

        warnIfStopped(canNotify = false)

        coVerify { preferencesManager.setReaderStoppedNotified(false) }
        verify { notificationManager.cancelReaderStoppedNotification() }
    }
}
