package com.meneses.budgethunter.sms.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SmsCounterpartExtractorTest {

    private fun extract(message: String) = SmsCounterpartExtractor.extract(message)

    @Test
    fun `finds the merchant of a Bancolombia purchase`() {
        assertEquals(
            "Exito Poblado",
            extract("Bancolombia le informa Compra por $50.000 en EXITO POBLADO desde su cuenta *1234.")
        )
    }

    @Test
    fun `keeps single letter words and stops at the card`() {
        assertEquals(
            "Genuino S Food",
            extract(
                "Bancolombia: Compraste COP71.000,00 en GENUINO S FOOD con tu T.Cred *2756, " +
                    "el 23/08/2026 a las 15:07. Si tienes dudas, encuentranos aqui: 6045109095."
            )
        )
    }

    @Test
    fun `keeps short connectors in lowercase`() {
        assertEquals(
            "Compañia Energetica de Occidente",
            extract(
                "Bancolombia: Pagaste $130,200 a COMPAÑIA ENERGETICA DE OCCIDENTE desde tu producto " +
                    "*4321 el 01/09/2026 a las 10:12."
            )
        )
    }

    @Test
    fun `finds who sent the money of an income`() {
        assertEquals(
            "Juan Perez",
            extract("Bancolombia: Recibiste $120.000 por Bre-B de JUAN PEREZ en tu cuenta *1234.")
        )
    }

    @Test
    fun `finds the merchant in the formats of other banks`() {
        assertEquals(
            "Rappi Colombia",
            extract("Davivienda le informa compra aprobada por $45.900 en RAPPI COLOMBIA con su tarjeta terminada en 4321.")
        )
        assertEquals(
            "Claro Colombia",
            extract("Scotiabank Colpatria: Pago de $89.900 a CLARO COLOMBIA aprobado desde tu cuenta *3456.")
        )
        assertEquals(
            "Drogueria Alemana",
            extract("AV Villas: Compra por $15.500 en DROGUERIA ALEMANA tarjeta *1122.")
        )
        assertEquals(
            "Carlos Ruiz",
            extract("Bco Popular: Transferencia recibida por $350.000 de CARLOS RUIZ en su cuenta *7890.")
        )
    }

    @Test
    fun `does not take a currency for a name`() {
        assertEquals("Exito", extract("Bancolombia: Pago de COP50.000 en EXITO aprobado."))
    }

    @Test
    fun `returns null when nobody is named in capitals`() {
        assertNull(extract("Bancolombia: Enviaste $80.000 por Bre-B a la llave @juan desde tu cuenta de ahorros *1234."))
        assertNull(extract("Bancolombia: Transferiste $30.000 desde tu cuenta *1234567 el 12/05."))
        assertNull(extract("Bancolombia: Compraste $10.000 con tu T.Cred *2756 en tienda."))
    }

    @Test
    fun `cuts long names on a whole word`() {
        val name = extract("Bancolombia: Compraste $10.000 en SUPERMERCADO MUY LARGO DEL NORTE DE LA CIUDAD CENTRO.")

        assertEquals("Supermercado Muy Largo del Norte", name)
    }
}
