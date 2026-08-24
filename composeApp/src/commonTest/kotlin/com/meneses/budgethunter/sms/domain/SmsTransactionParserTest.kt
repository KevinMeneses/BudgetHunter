package com.meneses.budgethunter.sms.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SmsTransactionParserTest {

    private val bankConfig = SupportedBanks.BANCOLOMBIA

    private fun analyze(message: String) = SmsTransactionParser.analyze(message, bankConfig)

    private data class Sample(
        val message: String,
        val amount: String,
        val type: BudgetEntry.Type,
        val description: String
    )

    private fun transaction(message: String): SmsAnalysis.Transaction {
        val analysis = analyze(message)
        assertIs<SmsAnalysis.Transaction>(analysis, "Expected a transaction for: $message")
        return analysis
    }

    @Test
    fun `read a regular purchase as an expense`() {
        val result = transaction(
            "Bancolombia le informa Compra por $50.000 en EXITO POBLADO desde su cuenta *1234."
        )

        assertEquals("50000", result.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, result.type)
    }

    @Test
    fun `read the amount even when the message mentions installments`() {
        val result = transaction(
            "Bancolombia: Compra por $99.900 en FALABELLA diferida a 12 cuotas."
        )

        assertEquals("99900", result.amount)
    }

    @Test
    fun `ignore masked account numbers when reading the amount`() {
        val result = transaction(
            "Bancolombia: Transferiste $30.000 desde tu cuenta *1234567 el 12/05."
        )

        assertEquals("30000", result.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, result.type)
    }

    @Test
    fun `read an outgoing Bre-B transfer as an expense`() {
        val result = transaction(
            "Bancolombia: Enviaste $80.000 por Bre-B a la llave @juan desde tu cuenta de ahorros *1234."
        )

        assertEquals("80000", result.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, result.type)
    }

    @Test
    fun `read an incoming Bre-B transfer as an income`() {
        val result = transaction(
            "Bancolombia: Recibiste $120.000 por Bre-B de JUAN PEREZ en tu cuenta *1234."
        )

        assertEquals("120000", result.amount)
        assertEquals(BudgetEntry.Type.INCOME, result.type)
    }

    @Test
    fun `describe a movement with its first sentence without the bank name`() {
        val result = transaction(
            "Bancolombia: Enviaste $80.000 por Bre-B a la llave @juan. Dudas al 018000912345."
        )

        assertEquals("Enviaste $80.000 por Bre-B a la llave @juan", result.description)
    }

    @Test
    fun `keep the dots of the amount and of the abbreviations inside the description`() {
        val result = transaction(
            "Bancolombia: Compraste COP71.000,00 en GENUINO S FOOD con tu T.Cred *2756, " +
                "el 23/08/2026 a las 15:07. Si tienes dudas, encuentranos aqui: 6045109095."
        )

        assertEquals(
            "Compraste COP71.000,00 en GENUINO S FOOD con tu T.Cred *2756, el 23/08/2026 a las 15:07",
            result.description
        )
    }

    @Test
    fun `read amount type and description from the messages the bank sends today`() {
        val samples = listOf(
            Sample(
                message = "Bancolombia: Pagaste $130,200 a COMPAÑIA ENERGETICA DE OCCIDENTE " +
                    "desde tu producto *4050 el 24/01/2026",
                amount = "130200",
                type = BudgetEntry.Type.OUTCOME,
                description = "Pagaste $130,200 a COMPAÑIA ENERGETICA DE OCCIDENTE desde tu producto *4050 el 24/01/2026"
            ),
            Sample(
                message = "Bancolombia: Transferiste $55,000.00 desde tu cuenta *4050 a la cuenta " +
                    "*3152271781 el 22/08/26 a las 13:30. ¿Dudas? Llamanos al 01800931987. Estamos cerca.",
                amount = "55000.00",
                type = BudgetEntry.Type.OUTCOME,
                description = "Transferiste $55,000.00 desde tu cuenta *4050 a la cuenta *3152271781 " +
                    "el 22/08/26 a las 13:30"
            ),
            Sample(
                message = "Bancolombia: Compraste COP71.000,00 en GENUINO S FOOD con tu T.Cred *2756, " +
                    "el 23/08/2026 a las 15:07. Si tienes dudas, encuentranos aqui: 6045109095 o " +
                    "018000931987. Estamos cerca.",
                amount = "71000.00",
                type = BudgetEntry.Type.OUTCOME,
                description = "Compraste COP71.000,00 en GENUINO S FOOD con tu T.Cred *2756, " +
                    "el 23/08/2026 a las 15:07"
            ),
            Sample(
                message = "Bancolombia: KEVIN, recibiste una transferencia de NUBIA ROCELY PALTA por " +
                    "$4,000,000 en tu cuenta *4050 conectada a la llave 1061771531 el 13/08/26 a las 12:36. " +
                    "Con llaves es de una y gratis. Dudas al 018000912345.",
                amount = "4000000",
                type = BudgetEntry.Type.INCOME,
                description = "KEVIN, recibiste una transferencia de NUBIA ROCELY PALTA por $4,000,000 " +
                    "en tu cuenta *4050 conectada a la llave 1061771531 el 13/08/26 a las 12:36"
            ),
            Sample(
                message = "Bancolombia: KEVIN, transferiste $108,000.00 a la llave 0092747406 desde tu " +
                    "cuenta *4050 a TERRAZALOUNGE el 16/08/26 a las 19:38. Con Bre-b es de una y gratis. " +
                    "Dudas al 018000912345.",
                amount = "108000.00",
                type = BudgetEntry.Type.OUTCOME,
                description = "KEVIN, transferiste $108,000.00 a la llave 0092747406 desde tu cuenta *4050 " +
                    "a TERRAZALOUNGE el 16/08/26 a las 19:38"
            ),
            Sample(
                message = "Bancolombia: KEVIN FELIPE MENESES PALTA pagaste $120,000.00 por codigo QR desde " +
                    "tu cuenta *4050 a la llave @cmytsas el 09/08/2026 a las 13:54. Con codigo QR es facil " +
                    "y de una. Dudas al 018000912345.",
                amount = "120000.00",
                type = BudgetEntry.Type.OUTCOME,
                description = "KEVIN FELIPE MENESES PALTA pagaste $120,000.00 por codigo QR desde tu cuenta " +
                    "*4050 a la llave @cmytsas el 09/08/2026 a las 13:54"
            ),
            Sample(
                message = "Bancolombia: Transferiste $67,900.00 por QR desde tu cuenta 4050 a la cuenta 0987, " +
                    "el 2026/04/23 19:07. ¿Dudas? Llamanos al 018000931987. Estamos cerca.",
                amount = "67900.00",
                type = BudgetEntry.Type.OUTCOME,
                description = "Transferiste $67,900.00 por QR desde tu cuenta 4050 a la cuenta 0987, " +
                    "el 2026/04/23 19:07"
            )
        )

        samples.forEach { sample ->
            val result = transaction(sample.message)
            assertEquals(sample.amount, result.amount, "amount of: ${sample.message}")
            assertEquals(sample.type, result.type, "type of: ${sample.message}")
            assertEquals(sample.description, result.description, "description of: ${sample.message}")
        }
    }

    @Test
    fun `read a Bre-B transfer without an explicit direction as an expense`() {
        val result = transaction(
            "Bancolombia le informa transferencia Bre-B por $45.000 a llave 3001234567."
        )

        assertEquals("45000", result.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, result.type)
    }

    @Test
    fun `read a received transfer as an income`() {
        val result = transaction(
            "Bancolombia: Recibiste una transferencia por $200.000 de MARIA GOMEZ a tu cuenta de ahorros."
        )

        assertEquals("200000", result.amount)
        assertEquals(BudgetEntry.Type.INCOME, result.type)
    }

    @Test
    fun `read a payroll deposit as an income`() {
        val result = transaction(
            "Bancolombia: Te consignaron $1.500.000 por nomina en tu cuenta de ahorros *1234."
        )

        assertEquals("1500000", result.amount)
        assertEquals(BudgetEntry.Type.INCOME, result.type)
    }

    @Test
    fun `discard a pre approved credit offer`() {
        val analysis = analyze(
            "Bancolombia: Felicitaciones, tienes un cupo preaprobado de $5.000.000. Solicitalo ya en la app."
        )

        assertEquals(SmsAnalysis.Ignored, analysis)
    }

    @Test
    fun `discard a promotional message`() {
        val analysis = analyze(
            "Bancolombia: Aprovecha y compra con tu tarjeta a 12 meses sin intereses por $1.000.000."
        )

        assertEquals(SmsAnalysis.Ignored, analysis)
    }

    @Test
    fun `discard a payment reminder`() {
        val analysis = analyze(
            "Bancolombia: Recuerda pagar tu tarjeta antes del 15. Pago minimo $150.000."
        )

        assertEquals(SmsAnalysis.Ignored, analysis)
    }

    @Test
    fun `discard a notice about the limit for card purchases`() {
        val analysis = analyze(
            "Bancolombia: Ups! Superaste el tope que tienes para compras con tus tarjetas en tiendas " +
                "físicas de $3,670,000. Cambialo cuando quieras en tu Sucursal Virtual Personas / " +
                "Seguridad / Modificar topes."
        )

        assertEquals(SmsAnalysis.Ignored, analysis)
    }

    @Test
    fun `discard a declined purchase`() {
        val analysis = analyze(
            "Bancolombia: Tu compra por $150.000 en EXITO fue rechazada."
        )

        assertEquals(SmsAnalysis.Ignored, analysis)
    }

    @Test
    fun `do not read a plural mention of purchases as a purchase`() {
        val analysis = analyze(
            "Bancolombia: Tu limite mensual para compras en linea es de $2.000.000."
        )

        assertEquals(SmsAnalysis.Unrecognized, analysis)
    }

    @Test
    fun `report a message whose wording is not covered`() {
        val analysis = analyze(
            "Bancolombia te informa que tu cupo disponible es de $2.000.000."
        )

        assertEquals(SmsAnalysis.Unrecognized, analysis)
    }

    @Test
    fun `report a fragment of a longer message`() {
        val analysis = analyze(
            "nuese a la linea 018000 912345 o desde tu celular al #012."
        )

        assertEquals(SmsAnalysis.Unrecognized, analysis)
    }

    @Test
    fun `report a movement without a readable amount`() {
        val analysis = analyze(
            "Bancolombia: Compra aprobada en EXITO. Consulta el detalle en la app."
        )

        assertEquals(SmsAnalysis.AmountNotFound, analysis)
    }

    @Test
    fun `report a movement written with an unlisted wording instead of dropping it`() {
        val analysis = analyze(
            "Bancolombia: Se realizo una operacion por $75.000 en tu cuenta de ahorros."
        )

        assertEquals(SmsAnalysis.Unrecognized, analysis)
    }

    @Test
    fun `discard a blank message`() {
        assertEquals(SmsAnalysis.Ignored, analyze("   "))
    }

    @Test
    fun `normalize colombian amounts using the dot as thousands separator`() {
        assertEquals("50000", SmsTransactionParser.normalizeAmount("50.000"))
        assertEquals("1234567", SmsTransactionParser.normalizeAmount("1.234.567"))
        assertEquals("560575.67", SmsTransactionParser.normalizeAmount("560.575,67"))
    }

    @Test
    fun `normalize american amounts using the comma as thousands separator`() {
        assertEquals("125678.00", SmsTransactionParser.normalizeAmount("125,678.00"))
        assertEquals("125678", SmsTransactionParser.normalizeAmount("125,678"))
    }

    @Test
    fun `normalize amounts with decimals`() {
        assertEquals("125.50", SmsTransactionParser.normalizeAmount("125.50"))
        assertEquals("50.5", SmsTransactionParser.normalizeAmount("50,5"))
    }
}
