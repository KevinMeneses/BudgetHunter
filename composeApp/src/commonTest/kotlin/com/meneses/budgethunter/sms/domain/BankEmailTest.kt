package com.meneses.budgethunter.sms.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Alert emails copied from real ones, as plain text: subject, greeting, movement and footer. */
class BankEmailTest {

    private val nequiEmail = """
        ¡Recibiste plata por Bre-B!

        ¡Hola, JUAN MENESES!

        Recibiste 525.000 de NUBIA ROCELY PALTA el 1 de noviembre de 2026 a las 11:11 a.m, desde el banco BBVA. Revisa el detalle en los movimientos de tu app o descarga el comprobante si lo necesitas.

        Si no fuiste tú, fue un error o tienes dudas, escríbenos al chat de nuestra página web: www.nequi.com.co en el botón de "Ayuda" o llama a la línea de atención: (+57) 300 600 0100

        ¿Ya nos sigues en redes? ¡Siempre tenemos cosas nuevas!
    """.trimIndent()

    private val luloEmail = """
        Conoce el detalle de la transacción

        Recibiste $65,680 de JUAN SEBASTIAN MENESES PALTA

        Origen cuenta • 1248
        Destino ahorro • 3936
        ID. transacción • 170280812

        Fecha 1 de noviembre de 2026
        Hora 11:24 a.m.

        © 2026. Lulo bank, Bogotá, Colombia.

        En Lulo bank, nunca te pediremos datos como claves o usuarios mediante correo electrónico. Si tienes alguna pregunta nos puedes contactar a través de tu app en la sección Ayuda o consulta www.lulobank.com
    """.trimIndent()

    private val bancolombiaEmail = """
        ¡Listo!
        Todo salió bien con tus movimientos

        Bancolombia: Pagaste $110,000.00 a GOU PAYMENTS S A EASPBV desde tu producto 4050 el 05/11/2026 18:03:38. ¿Dudas? Llamanos al 6045109095. Estamos cerca

        ¿Quieres ver el equilibrio de tus gastos y tus ingresos? Haz eso y mucho más desde Día a Día en nuestra app Mi Bancolombia.
    """.trimIndent()

    private fun transaction(email: String): Pair<BankSmsConfig, SmsAnalysis.Transaction> {
        val passage = assertNotNull(BankEmail.movementPassage(email), "Expected a movement in: $email")
        val bank = assertNotNull(BankEmail.bankOf(email, SupportedBanks.SHORTCUT_BANKS), "Expected a bank in: $email")
        val analysis = SmsTransactionParser.analyze(passage, bank)
        assertIs<SmsAnalysis.Transaction>(analysis, "Expected a transaction for: $passage")
        return bank to analysis
    }

    @Test
    fun `a Nequi email is read as Nequi even though it names the sending bank`() {
        val (bank, result) = transaction(nequiEmail)

        assertEquals(SupportedBanks.NEQUI, bank)
        assertEquals("525000", result.amount)
        assertEquals(BudgetEntry.Type.INCOME, result.type)
        assertEquals(
            "Recibiste 525.000 de NUBIA ROCELY PALTA el 1 de noviembre de 2026 a las 11:11 a.m, desde el banco BBVA",
            result.description
        )
    }

    @Test
    fun `a Lulo bank email is read by its domain`() {
        val (bank, result) = transaction(luloEmail)

        assertEquals(SupportedBanks.LULO_BANK, bank)
        assertEquals("65680", result.amount)
        assertEquals(BudgetEntry.Type.INCOME, result.type)
        assertEquals("Recibiste $65,680 de JUAN SEBASTIAN MENESES PALTA", result.description)
    }

    @Test
    fun `a Bancolombia email is read by its name`() {
        val (bank, result) = transaction(bancolombiaEmail)

        assertEquals(SupportedBanks.BANCOLOMBIA, bank)
        assertEquals("110000.00", result.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, result.type)
    }

    @Test
    fun `the movement sentence skips the subject, the greeting and the footer`() {
        assertEquals(
            "Recibiste $65,680 de JUAN SEBASTIAN MENESES PALTA",
            BankEmail.movementPassage(luloEmail)
        )
    }

    @Test
    fun `an email without a movement has no passage`() {
        val newsletter = """
            ¡Hola, JUAN!

            Conoce las novedades de tu app.
            Activa tus alertas desde la sección Ayuda.
        """.trimIndent()

        assertNull(BankEmail.movementPassage(newsletter))
    }

    @Test
    fun `an SMS is not taken for an email`() {
        assertFalse(
            BankEmail.looksLikeEmail("Bancolombia: Compraste COP25.000,00 en EXITO con tu T.Deb *1234.")
        )
        assertTrue(BankEmail.looksLikeEmail(nequiEmail))
        assertTrue(BankEmail.looksLikeEmail(bancolombiaEmail))
    }

    @Test
    fun `email banks are offered to the shortcut but not to the Android selector`() {
        assertTrue(SupportedBanks.LULO_BANK in SupportedBanks.SHORTCUT_BANKS)
        assertTrue(SupportedBanks.NEQUI in SupportedBanks.SHORTCUT_BANKS)
        assertFalse(SupportedBanks.LULO_BANK in SupportedBanks.SELECTABLE_BANKS)
    }

    @Test
    fun `a domain only counts as a whole address part and the first one wins`() {
        assertNull(BankEmail.bankOf("Visita xnequi.com.co hoy", listOf(SupportedBanks.NEQUI)))
        assertEquals(
            SupportedBanks.NEQUI,
            BankEmail.bankOf("De notificaciones@nequi.com.co sobre lulobank.com", SupportedBanks.SHORTCUT_BANKS)
        )
    }
}
