package com.meneses.budgethunter.sms.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Notifications copied from the apps, title and text as Android hands them over. */
class BankNotificationTest {

    private fun transaction(notification: BankNotification, bankConfig: BankSmsConfig): SmsAnalysis.Transaction {
        val analysis = SmsTransactionParser.analyze(notification.message, bankConfig)
        assertIs<SmsAnalysis.Transaction>(analysis, "Expected a transaction for: ${notification.message}")
        return analysis
    }

    @Test
    fun `read money received in Nequi as income`() {
        val notification = BankNotification(
            title = "Envío",
            text = "VALENTINA ARCINIEGAS te envió 1000, ¡lo mejor!"
        )

        val result = transaction(notification, SupportedBanks.NEQUI)

        assertEquals("1000", result.amount)
        assertEquals(BudgetEntry.Type.INCOME, result.type)
        assertEquals("VALENTINA ARCINIEGAS te envió 1000, ¡lo mejor!", notification.description)
    }

    @Test
    fun `read money sent from Nequi as an expense`() {
        val notification = BankNotification(
            title = "Envío de plata exitoso",
            text = "Te contamos que el envío de plata por $2.000 fue exitoso. Puedes revisar en tus movimientos."
        )

        val result = transaction(notification, SupportedBanks.NEQUI)

        assertEquals("2000", result.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, result.type)
        assertEquals("Te contamos que el envío de plata por $2.000 fue exitoso", notification.description)
    }

    @Test
    fun `read money received in Nu as income`() {
        val notification = BankNotification(
            title = "Recibiste 2.000,00 en tu cuenta",
            text = "Te llegó dinero de KEVIN FELIPE MENESES PALTA con tu llave."
        )

        val result = transaction(notification, SupportedBanks.NU)

        assertEquals("2000.00", result.amount)
        assertEquals(BudgetEntry.Type.INCOME, result.type)
        assertEquals("Te llegó dinero de KEVIN FELIPE MENESES PALTA con tu llave", notification.description)
    }

    @Test
    fun `read money sent from Nu as an expense`() {
        val notification = BankNotification(
            title = "Enviaste $1.952,00",
            text = "Le enviaste a KEV**** MEN****** en su cuenta de Bancolombia."
        )

        val result = transaction(notification, SupportedBanks.NU)

        assertEquals("1952.00", result.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, result.type)
        assertEquals("Le enviaste a KEV**** MEN****** en su cuenta de Bancolombia", notification.description)
    }

    @Test
    fun `a notification that reports no movement is not taken for one`() {
        val notification = BankNotification(
            title = "Nequi",
            text = "Ya puedes usar tu nueva tarjeta en cualquier comercio"
        )

        val analysis = SmsTransactionParser.analyze(notification.message, SupportedBanks.NEQUI)

        assertIs<SmsAnalysis.Unrecognized>(analysis)
    }

    @Test
    fun `the title describes the entry when the app posts no text`() {
        assertEquals("Enviaste $1.952,00", BankNotification(title = "Enviaste $1.952,00", text = "").description)
        assertEquals("Enviaste $1.952,00", BankNotification(title = "Enviaste $1.952,00.", text = "").message)
    }

    @Test
    fun `a title that closes its own sentence is not given a second dot`() {
        assertEquals("¡Listo! Enviaste $2.000", BankNotification(title = "¡Listo!", text = "Enviaste $2.000").message)
        assertEquals("Envío. Te envió 1000", BankNotification(title = "Envío.", text = "Te envió 1000").message)
        assertEquals("Te envió 1000", BankNotification(title = "", text = "Te envió 1000").message)
    }

    @Test
    fun `only the apps of supported banks are read`() {
        assertEquals(SupportedBanks.BBVA_COLOMBIA, SupportedBanks.getNotificationBankByPackage("co.com.bbva.mb"))
        assertEquals(SupportedBanks.NEQUI, SupportedBanks.getNotificationBankByPackage("com.nequi.MobileApp"))
        assertEquals(SupportedBanks.NU, SupportedBanks.getNotificationBankByPackage("com.nu.production"))
        assertNull(SupportedBanks.getNotificationBankByPackage("com.whatsapp"))
        assertNull(SupportedBanks.getNotificationBankByPackage("com.meneses.budgethunter"))
    }

    @Test
    fun `notification banks share the selection with SMS banks but never reach the SMS receiver`() {
        assertTrue(SupportedBanks.NEQUI in SupportedBanks.SELECTABLE_BANKS)
        assertTrue(SupportedBanks.NU in SupportedBanks.SELECTABLE_BANKS)
        assertTrue(SupportedBanks.BANCOLOMBIA in SupportedBanks.SELECTABLE_BANKS)
        assertEquals(SupportedBanks.NEQUI, SupportedBanks.getSelectableBankById("nequi"))
        assertNull(SupportedBanks.getBankConfigById("nequi"))
        assertNull(SupportedBanks.getBankConfigById("nu"))
        // BBVA notifies in its app only, so the SMS receiver never tries it
        assertNull(SupportedBanks.getBankConfigById("bbva_colombia"))
        assertEquals(SupportedBanks.BBVA_COLOMBIA, SupportedBanks.getSelectableBankById("bbva_colombia"))
    }

    @Test
    fun `notification banks are the selectable banks with app packages, each listed once`() {
        assertEquals(
            listOf(SupportedBanks.BBVA_COLOMBIA, SupportedBanks.NEQUI, SupportedBanks.NU),
            SupportedBanks.NOTIFICATION_BANKS
        )
        assertEquals(
            SupportedBanks.SELECTABLE_BANKS.size,
            SupportedBanks.SELECTABLE_BANKS.map { it.id }.toSet().size
        )
        assertTrue(SupportedBanks.NEQUI.readsNotifications)
        assertFalse(SupportedBanks.NEQUI.readsSms)
        assertTrue(SupportedBanks.BANCOLOMBIA.readsSms)
        assertFalse(SupportedBanks.BANCOLOMBIA.readsNotifications)
    }

    @Test
    fun `a reposted notification is handled once`() {
        val recent = RecentNotifications(capacity = 2)

        assertTrue(recent.markIfNew("a"))
        assertFalse(recent.markIfNew("a"))
        assertTrue(recent.markIfNew("b"))
        assertTrue(recent.markIfNew("c"))
        // "a" fell out of the window
        assertTrue(recent.markIfNew("a"))
    }
}
