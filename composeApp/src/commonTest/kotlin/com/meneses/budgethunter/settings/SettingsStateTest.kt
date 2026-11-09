package com.meneses.budgethunter.settings

import com.meneses.budgethunter.settings.application.SettingsState
import com.meneses.budgethunter.sms.domain.ReadingChannel
import com.meneses.budgethunter.sms.domain.SupportedBanks
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsStateTest {

    private val allOn = SettingsState(
        isSmsReadingEnabled = true,
        isBankNotificationReadingEnabled = true,
        isBankEmailReadingEnabled = true
    )

    @Test
    fun `switches on for different banks do not repeat movements`() {
        val state = allOn.copy(selectedBanks = setOf(SupportedBanks.BANCOLOMBIA, SupportedBanks.NU))

        assertFalse(state.mayRepeatMovements)
    }

    @Test
    fun `a bank read from two channels that are on may repeat movements`() {
        val state = allOn.copy(
            selectedBanks = setOf(SupportedBanks.BANCOLOMBIA),
            bankChannels = mapOf("bancolombia" to setOf(ReadingChannel.SMS, ReadingChannel.EMAIL))
        )

        assertTrue(state.mayRepeatMovements)
        assertFalse(state.copy(isBankEmailReadingEnabled = false).mayRepeatMovements)
    }

    @Test
    fun `a channel picked for a bank is missing while its switch is off`() {
        val state = SettingsState(
            isSmsReadingEnabled = true,
            selectedBanks = setOf(SupportedBanks.BANCOLOMBIA),
            bankChannels = mapOf("bancolombia" to setOf(ReadingChannel.SMS, ReadingChannel.EMAIL))
        )

        assertTrue(state.isMissing(ReadingChannel.EMAIL))
        assertFalse(state.isMissing(ReadingChannel.SMS))
        assertFalse(state.isMissing(ReadingChannel.APP))
    }
}
