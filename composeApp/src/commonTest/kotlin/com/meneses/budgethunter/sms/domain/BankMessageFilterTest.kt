package com.meneses.budgethunter.sms.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BankMessageFilterTest {

    @Test
    fun `splits the bank numbers from the names it signs with`() {
        val filter = BankMessageFilter.of(SupportedBanks.BANCOLOMBIA)

        assertEquals("Bancolombia", filter.bankName)
        assertEquals(listOf("85784", "87400"), filter.senders)
        assertEquals(listOf("Bancolombia"), filter.keywords)
    }

    @Test
    fun `keeps names that differ in more than case`() {
        val filter = BankMessageFilter.of(SupportedBanks.SCOTIABANK_COLPATRIA)

        assertEquals(listOf("85777"), filter.senders)
        assertEquals(listOf("Scotiabank Colpatria", "COLPATRIA", "Scotiabank"), filter.keywords)
    }

    @Test
    fun `there is a filter for every supported bank`() {
        assertEquals(SupportedBanks.ALL_BANKS.map { it.displayName }, BankMessageFilter.ALL.map { it.bankName })
        assertTrue(BankMessageFilter.ALL.all { it.keywords.isNotEmpty() })
    }
}
