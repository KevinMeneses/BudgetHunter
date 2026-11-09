package com.meneses.budgethunter.sms.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class BankChannelsTest {

    @Test
    fun `a bank offers its emails besides the channels it has`() {
        assertEquals(
            listOf(ReadingChannel.SMS, ReadingChannel.EMAIL),
            BankChannels.available(SupportedBanks.BANCOLOMBIA)
        )
        assertEquals(listOf(ReadingChannel.APP, ReadingChannel.EMAIL), BankChannels.available(SupportedBanks.NU))
    }

    @Test
    fun `a bank never picked is read as before, never by email`() {
        assertEquals(setOf(ReadingChannel.SMS), BankChannels.of(SupportedBanks.BANCOLOMBIA, emptyMap()))
        assertEquals(setOf(ReadingChannel.APP), BankChannels.of(SupportedBanks.NEQUI, emptyMap()))
    }

    @Test
    fun `several channels can be picked for one bank`() {
        val chosen = mapOf("bancolombia" to setOf(ReadingChannel.SMS, ReadingChannel.EMAIL))

        assertEquals(
            setOf(ReadingChannel.SMS, ReadingChannel.EMAIL),
            BankChannels.of(SupportedBanks.BANCOLOMBIA, chosen)
        )
    }

    @Test
    fun `a channel the bank does not have is ignored`() {
        val chosen = mapOf("bancolombia" to setOf(ReadingChannel.APP, ReadingChannel.EMAIL))

        assertEquals(setOf(ReadingChannel.EMAIL), BankChannels.of(SupportedBanks.BANCOLOMBIA, chosen))
    }

    @Test
    fun `a bank can be kept with no channel, read from nothing`() {
        val chosen = mapOf("bancolombia" to emptySet<ReadingChannel>())

        assertEquals(emptySet(), BankChannels.of(SupportedBanks.BANCOLOMBIA, chosen))
        assertEquals(chosen, BankChannels.decode(BankChannels.encode(chosen)))
    }

    @Test
    fun `the switches of earlier versions become channels without changing what is read`() {
        val selected = listOf(SupportedBanks.BANCOLOMBIA, SupportedBanks.NU, SupportedBanks.DAVIVIENDA)
        val stored = mapOf("davivienda" to setOf(ReadingChannel.SMS, ReadingChannel.EMAIL))

        val migrated = BankChannels.migrate(selected, stored, isSmsOn = true, isAppOn = false, isEmailOn = false)

        assertEquals(
            mapOf(
                "bancolombia" to setOf(ReadingChannel.SMS),
                // Its app was not read: it stays selected, read from nothing
                "nu" to emptySet(),
                "davivienda" to setOf(ReadingChannel.SMS)
            ),
            migrated
        )
    }

    @Test
    fun `the channels in use are those of any bank`() {
        val chosen = mapOf("nu" to setOf(ReadingChannel.EMAIL))

        assertEquals(
            setOf(ReadingChannel.SMS, ReadingChannel.EMAIL),
            BankChannels.inUse(listOf(SupportedBanks.BANCOLOMBIA, SupportedBanks.NU), chosen)
        )
    }

    @Test
    fun `only the banks read from a channel are tried for it`() {
        val chosen = mapOf(
            "bancolombia" to setOf(ReadingChannel.EMAIL),
            "davivienda" to setOf(ReadingChannel.SMS, ReadingChannel.EMAIL)
        )
        val banks = listOf(SupportedBanks.BANCOLOMBIA, SupportedBanks.DAVIVIENDA, SupportedBanks.NU)

        assertEquals(listOf(SupportedBanks.DAVIVIENDA), BankChannels.readBy(ReadingChannel.SMS, banks, chosen))
        assertEquals(
            listOf(SupportedBanks.BANCOLOMBIA, SupportedBanks.DAVIVIENDA),
            BankChannels.readBy(ReadingChannel.EMAIL, banks, chosen)
        )
        assertEquals(listOf(SupportedBanks.NU), BankChannels.readBy(ReadingChannel.APP, banks, chosen))
    }

    @Test
    fun `the choice survives being stored`() {
        val chosen = mapOf(
            "bancolombia" to setOf(ReadingChannel.SMS, ReadingChannel.EMAIL),
            "nu" to setOf(ReadingChannel.APP)
        )

        assertEquals(chosen, BankChannels.decode(BankChannels.encode(chosen)))
        assertEquals(emptyMap(), BankChannels.decode(setOf("garbage", ":SMS", "nu:FAX")))
    }
}
