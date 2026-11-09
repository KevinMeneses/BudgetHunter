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
    fun `an expanded text that does not repeat the subject is kept whole`() {
        val email = BankEmail.fromNotification(
            title = "Davivienda",
            text = "Notificación de transacción",
            bigText = "Realizaste una compra por \$20.000 en D1."
        )

        assertEquals("Realizaste una compra por \$20.000 en D1.", email.body)
        assertEquals("Realizaste una compra por \$20.000 en D1", email.toNotification().description)
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
    fun `without an address the sender is matched against the selected banks by name`() {
        val selected = listOf(SupportedBanks.BANCOLOMBIA, SupportedBanks.DAVIVIENDA, SupportedBanks.NU)

        assertEquals(SupportedBanks.BANCOLOMBIA, BankEmail.findSender("Bancolombia", emptyList(), selected))
        assertEquals(SupportedBanks.DAVIVIENDA, BankEmail.findSender("Notificaciones Davivienda", emptyList(), selected))
        assertEquals(SupportedBanks.NU, BankEmail.findSender("Nu Colombia", emptyList(), selected))
        // Case and accents do not matter
        assertEquals(SupportedBanks.BANCOLOMBIA, BankEmail.findSender("ALERTAS BANCOLOMBIA", emptyList(), selected))
        assertEquals(
            SupportedBanks.BANCAMIA,
            BankEmail.findSender("Bancamía", emptyList(), listOf(SupportedBanks.BANCAMIA))
        )
        assertEquals(
            SupportedBanks.BANCAMIA,
            BankEmail.findSender("bancamia", emptyList(), listOf(SupportedBanks.BANCAMIA))
        )
        // A bank that is not selected, a person or a store are not read
        assertNull(BankEmail.findSender("BBVA", emptyList(), selected))
        assertNull(BankEmail.findSender("Nubia Rodríguez", emptyList(), selected))
        assertNull(BankEmail.findSender("Rappi", emptyList(), selected))
        assertNull(BankEmail.findSender("", emptyList(), selected))
    }

    @Test
    fun `an address from the bank's domain proves the sender whatever its name`() {
        val selected = listOf(SupportedBanks.BANCOLOMBIA, SupportedBanks.DAVIVIENDA)

        assertEquals(
            SupportedBanks.BANCOLOMBIA,
            BankEmail.findSender(
                "Alertas y Notificaciones",
                listOf("mailto:alertasynotificaciones@an.notificacionesbancolombia.com"),
                selected
            )
        )
        assertEquals(
            SupportedBanks.DAVIVIENDA,
            BankEmail.findSender("Davivienda", listOf("Davivienda <notificaciones@davivienda.com>"), selected)
        )
    }

    @Test
    fun `a bank's name on an address outside its domains is a fake`() {
        val selected = listOf(SupportedBanks.BANCOLOMBIA)

        assertNull(BankEmail.findSender("Bancolombia", listOf("mailto:alertas@bancolombia-seguro.co"), selected))
        // A lookalike domain is not under the bank's
        assertNull(BankEmail.findSender("Bancolombia", listOf("alertas@fakebancolombia.com.co"), selected))
    }

    @Test
    fun `a bank with no known domain is still told by its name`() {
        assertEquals(
            SupportedBanks.BANCAMIA,
            BankEmail.findSender("Bancamía", listOf("mailto:avisos@correo.example"), listOf(SupportedBanks.BANCAMIA))
        )
    }

    @Test
    fun `only email addresses give a domain`() {
        assertEquals("bancolombia.com.co", BankEmail.addressDomain("mailto:a@Bancolombia.com.co"))
        assertEquals("davivienda.com", BankEmail.addressDomain("Davivienda <x@davivienda.com>"))
        assertNull(BankEmail.addressDomain("tel:+573001234567"))
        assertNull(BankEmail.addressDomain("content://com.android.contacts/contacts/lookup/1"))
        assertNull(BankEmail.addressDomain(""))
    }

    @Test
    fun `only known email apps are looked at`() {
        assertTrue(BankEmail.isEmailApp("com.google.android.gm"))
        assertTrue(BankEmail.isEmailApp("com.microsoft.office.outlook"))
        assertFalse(BankEmail.isEmailApp("com.whatsapp"))
        assertFalse(BankEmail.isEmailApp(null))
    }
}
