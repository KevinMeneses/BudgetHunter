package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import com.meneses.budgethunter.sms.domain.BankEmail
import com.meneses.budgethunter.sms.domain.SmsParseResult
import com.meneses.budgethunter.sms.domain.SupportedBanks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** Banks email far more news than movements: only what the user can act on is notified. */
class ProcessBankEmailTest {

    private val registerSmsEntry = mockk<RegisterSmsEntryUseCase>()
    private val notificationManager = mockk<NotificationManager>(relaxed = true)
    private val stringResourceProvider = mockk<StringResourceProvider>(relaxed = true)
    private val processSms = ProcessSmsUseCase(registerSmsEntry, notificationManager, stringResourceProvider)

    private val email = BankEmail(sender = "Bancolombia", subject = "Alertas", body = "Bancolombia le informa Compra")

    @Test
    fun `a movement with no default budget leads to the settings`() = runTest {
        coEvery { registerSmsEntry.registerNotification(any(), any()) } returns SmsParseResult.NoDefaultBudget

        processSms.processBankEmail(email, SupportedBanks.BANCOLOMBIA)

        verify { notificationManager.showOpenSettingsNotification(any(), any()) }
    }

    @Test
    fun `an email that is not a readable movement is dropped without a word`() = runTest {
        for (result in listOf(SmsParseResult.Unrecognized, SmsParseResult.AmountNotFound, SmsParseResult.Ignored)) {
            coEvery { registerSmsEntry.registerNotification(any(), any()) } returns result

            processSms.processBankEmail(email, SupportedBanks.BANCOLOMBIA)
        }

        verify(exactly = 0) { notificationManager.showNotification(any(), any()) }
        verify(exactly = 0) { notificationManager.showOpenSettingsNotification(any(), any()) }
        verify(exactly = 0) { notificationManager.showEntryAddedNotification(any(), any()) }
    }
}
