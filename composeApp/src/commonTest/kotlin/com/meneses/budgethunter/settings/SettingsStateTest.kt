package com.meneses.budgethunter.settings

import com.meneses.budgethunter.settings.application.SettingsState
import com.meneses.budgethunter.sms.domain.ReadingChannel
import com.meneses.budgethunter.sms.domain.SupportedBanks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsStateTest {

    @Test
    fun `the channels in use decide which permissions are needed`() {
        val state = SettingsState(
            selectedBanks = setOf(SupportedBanks.BANCOLOMBIA, SupportedBanks.NU),
            bankChannels = mapOf("nu" to setOf(ReadingChannel.EMAIL))
        )

        assertEquals(setOf(ReadingChannel.SMS, ReadingChannel.EMAIL), state.channelsInUse)
        assertTrue(state.isSmsPermissionMissing)
        assertFalse(state.copy(hasSmsPermission = true).isSmsPermissionMissing)
        assertTrue(state.isNotificationReaderNeeded)
    }

    @Test
    fun `banks read from no channel need nothing`() {
        val state = SettingsState(
            selectedBanks = setOf(SupportedBanks.BANCOLOMBIA),
            bankChannels = mapOf("bancolombia" to emptySet())
        )

        assertFalse(state.isAnyReadingEnabled)
        assertFalse(state.isSmsPermissionMissing)
        assertFalse(state.isNotificationReaderNeeded)
    }

    @Test
    fun `banks are listed by name and only the others can be added`() {
        val state = SettingsState(
            availableBanks = SupportedBanks.SELECTABLE_BANKS,
            selectedBanks = setOf(SupportedBanks.NU, SupportedBanks.BANCOLOMBIA)
        )

        assertEquals(listOf(SupportedBanks.BANCOLOMBIA, SupportedBanks.NU), state.sortedSelectedBanks)
        assertFalse(SupportedBanks.NU in state.unselectedBanks)
        assertTrue(SupportedBanks.NEQUI in state.unselectedBanks)
    }
}
