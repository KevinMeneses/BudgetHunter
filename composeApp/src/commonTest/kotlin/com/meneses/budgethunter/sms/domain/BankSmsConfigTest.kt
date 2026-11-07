package com.meneses.budgethunter.sms.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class BankSmsConfigTest {

    @Test
    fun `a description that does not name the bank opens with it`() {
        assertEquals(
            "Nequi: VALENTINA ARCINIEGAS te envió 1000",
            SupportedBanks.NEQUI.describe("VALENTINA ARCINIEGAS te envió 1000")
        )
    }

    @Test
    fun `a description that names the bank is kept as it is`() {
        val text = "Bancolombia le informa Compra por $50.000 en EXITO"

        assertEquals(text, SupportedBanks.BANCOLOMBIA.describe(text))
    }

    @Test
    fun `the bank is found without its accents and by a sender keyword`() {
        assertEquals("BANCAMIA: compra por 20.000", SupportedBanks.BANCAMIA.describe("BANCAMIA: compra por 20.000"))
        assertEquals("Compra AV Villas por 10.000", SupportedBanks.BANCO_AV_VILLAS.describe("Compra AV Villas por 10.000"))
    }

    @Test
    fun `another bank named in the text does not count`() {
        assertEquals(
            "Nu: Le enviaste a KEV**** en su cuenta de Bancolombia",
            SupportedBanks.NU.describe("Le enviaste a KEV**** en su cuenta de Bancolombia")
        )
    }

    @Test
    fun `the bank name counts only as a whole word`() {
        assertEquals("Nu: Tu nueva tarjeta fue usada", SupportedBanks.NU.describe("Tu nueva tarjeta fue usada"))
    }

    @Test
    fun `a sender number is not taken for the bank name`() {
        assertEquals(
            "Bancolombia: Pago desde 85784 por 1.000",
            SupportedBanks.BANCOLOMBIA.describe("Pago desde 85784 por 1.000")
        )
    }

    @Test
    fun `an empty description becomes the bank name`() {
        assertEquals("Nu", SupportedBanks.NU.describe(" "))
    }
}
