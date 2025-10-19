package com.meneses.budgethunter.sms.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupportedBanksTest {

    @Test
    fun `getBankConfigById returns matching configuration`() {
        val config = SupportedBanks.getBankConfigById("bancolombia")

        assertEquals("Bancolombia", config?.displayName)
        assertTrue(config?.senderKeywords?.contains("Bancolombia") == true)
    }

    @Test
    fun `getBankConfigById returns null for unknown identifier`() {
        val config = SupportedBanks.getBankConfigById("unknown_bank")

        assertNull(config)
    }

    @Test
    fun `all banks are sorted alphabetically by display name`() {
        val isSorted = SupportedBanks.ALL_BANKS
            .zipWithNext { current, next -> current.displayName <= next.displayName }
            .all { it }

        assertTrue(isSorted)
    }
}
