package com.meneses.budgethunter.sms.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Email notifications as email apps post them: sender as the title, subject as the text. */
class BankEmailTest {

    @Test
    fun `the subject repeated on the first line of the expanded text is taken off the body`() {
        val email = BankEmail.fromNotification(
            title = "Bancolombia",
            text = "Alertas y Notificaciones",
            bigText = "Alertas y Notificaciones\nBancolombia le informa Compra por \$50.000,00 en EXITO\nCALLE 80. Hora 10:12"
        )

        assertEquals("Bancolombia", email.sender)
        assertEquals("Alertas y Notificaciones", email.subject)
        assertEquals("Bancolombia le informa Compra por \$50.000,00 en EXITO CALLE 80. Hora 10:12", email.body)
    }

    @Test
    fun `without an expanded text the subject is all there is`() {
        val email = BankEmail.fromNotification(title = "Davivienda", text = "Compra aprobada por \$20.000", bigText = null)

        assertEquals("Compra aprobada por \$20.000", email.subject)
        assertEquals("", email.body)
        assertFalse(email.isBlank)
    }

    @Test
    fun `a purchase emailed by the bank is read as an expense described by the body`() {
        val email = BankEmail.fromNotification(
            title = "Bancolombia",
            text = "Alertas y Notificaciones",
            bigText = "Alertas y Notificaciones\nBancolombia le informa Compra por \$50.000,00 en EXITO. Hora 10:12"
        )
        val notification = email.toNotification()

        val analysis = SmsTransactionParser.analyze(notification.message, SupportedBanks.BANCOLOMBIA)

        assertIs<SmsAnalysis.Transaction>(analysis)
        assertEquals("50000.00", analysis.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, analysis.type)
        assertEquals("Bancolombia le informa Compra por \$50.000,00 en EXITO", notification.description)
    }

    @Test
    fun `the sender is matched against the selected banks by name`() {
        val selected = listOf(SupportedBanks.BANCOLOMBIA, SupportedBanks.DAVIVIENDA, SupportedBanks.NU)

        assertEquals(SupportedBanks.BANCOLOMBIA, BankEmail.findSender("Bancolombia", selected))
        assertEquals(SupportedBanks.DAVIVIENDA, BankEmail.findSender("Notificaciones Davivienda", selected))
        assertEquals(SupportedBanks.NU, BankEmail.findSender("Nu Colombia", selected))
        // A bank that is not selected, a person or a store are not read
        assertNull(BankEmail.findSender("BBVA", selected))
        assertNull(BankEmail.findSender("Nubia Rodríguez", selected))
        assertNull(BankEmail.findSender("Rappi", selected))
        assertNull(BankEmail.findSender("", selected))
    }

    @Test
    fun `only known email apps are looked at`() {
        assertTrue(BankEmail.isEmailApp("com.google.android.gm"))
        assertTrue(BankEmail.isEmailApp("com.microsoft.office.outlook"))
        assertFalse(BankEmail.isEmailApp("com.whatsapp"))
        assertFalse(BankEmail.isEmailApp(null))
    }
}
