package com.meneses.budgethunter.sms.application

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.sms_shortcut_error
import budgethunter.composeapp.generated.resources.sms_shortcut_expense_registered
import budgethunter.composeapp.generated.resources.sms_shortcut_expense_registered_at
import budgethunter.composeapp.generated.resources.sms_shortcut_ignored
import budgethunter.composeapp.generated.resources.sms_shortcut_income_registered_from
import budgethunter.composeapp.generated.resources.sms_shortcut_no_budget
import budgethunter.composeapp.generated.resources.sms_shortcut_unrecognized
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import com.meneses.budgethunter.sms.domain.SmsParseResult
import com.meneses.budgethunter.sms.domain.SupportedBanks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegisterSmsFromShortcutUseCaseTest {

    private val registerSmsEntry = mockk<RegisterSmsEntryUseCase>()
    private val stringResourceProvider = mockk<StringResourceProvider>()

    private val registerSmsFromShortcut = RegisterSmsFromShortcutUseCase(registerSmsEntry, stringResourceProvider)

    private fun givenResult(message: String, result: SmsParseResult) {
        coEvery { registerSmsEntry(message, SupportedBanks.ALL_BANKS) } returns result
    }

    @Test
    fun `answers with the amount and the merchant of an expense`() = runTest {
        val message = "Bancolombia: Compraste COP25.000,00 en EXITO con tu T.Deb *1234."
        givenResult(message, SmsParseResult.Success(BudgetEntry(amount = "25000.00", budgetId = 7)))
        coEvery {
            stringResourceProvider.getString(Res.string.sms_shortcut_expense_registered_at, "$25,000", "Exito")
        } returns "Registrado: $25,000 en Exito"

        val reply = registerSmsFromShortcut(message)

        assertTrue(reply.isRegistered)
        assertEquals("Registrado: $25,000 en Exito", reply.message)
    }

    @Test
    fun `answers with the amount alone when no merchant is named`() = runTest {
        val message = "Bancolombia: Transferiste $30.000 desde tu cuenta *1234567 el 12/05."
        givenResult(message, SmsParseResult.Success(BudgetEntry(amount = "30000", budgetId = 7)))
        coEvery {
            stringResourceProvider.getString(Res.string.sms_shortcut_expense_registered, "$30,000")
        } returns "Registrado: gasto de $30,000"

        val reply = registerSmsFromShortcut(message)

        assertTrue(reply.isRegistered)
        assertEquals("Registrado: gasto de $30,000", reply.message)
    }

    @Test
    fun `answers who sent the money of an income`() = runTest {
        val message = "Bancolombia: Recibiste $120.000 por Bre-B de JUAN PEREZ en tu cuenta *1234."
        givenResult(
            message,
            SmsParseResult.Success(BudgetEntry(amount = "120000", type = BudgetEntry.Type.INCOME, budgetId = 7))
        )
        coEvery {
            stringResourceProvider.getString(Res.string.sms_shortcut_income_registered_from, "$120,000", "Juan Perez")
        } returns "Registrado: ingreso de $120,000 de Juan Perez"

        val reply = registerSmsFromShortcut(message)

        assertTrue(reply.isRegistered)
        assertEquals("Registrado: ingreso de $120,000 de Juan Perez", reply.message)
    }

    @Test
    fun `answers that it could not read the message`() = runTest {
        givenResult("unreadable", SmsParseResult.Unrecognized)
        givenResult("no amount", SmsParseResult.AmountNotFound)
        coEvery { stringResourceProvider.getString(Res.string.sms_shortcut_unrecognized) } returns "No pude interpretar el mensaje"

        assertEquals(SmsShortcutReply(false, "No pude interpretar el mensaje"), registerSmsFromShortcut("unreadable"))
        assertEquals(SmsShortcutReply(false, "No pude interpretar el mensaje"), registerSmsFromShortcut("no amount"))
    }

    @Test
    fun `answers that advertising was not registered`() = runTest {
        givenResult("promo", SmsParseResult.Ignored)
        coEvery { stringResourceProvider.getString(Res.string.sms_shortcut_ignored) } returns "ignored"

        val reply = registerSmsFromShortcut("promo")

        assertFalse(reply.isRegistered)
        assertEquals("ignored", reply.message)
    }

    @Test
    fun `asks for a default budget when there is none`() = runTest {
        givenResult("purchase", SmsParseResult.NoDefaultBudget)
        coEvery { stringResourceProvider.getString(Res.string.sms_shortcut_no_budget) } returns "no budget"

        val reply = registerSmsFromShortcut("purchase")

        assertFalse(reply.isRegistered)
        assertEquals("no budget", reply.message)
    }

    @Test
    fun `answers with an error instead of failing the shortcut`() = runTest {
        coEvery { registerSmsEntry("purchase", SupportedBanks.ALL_BANKS) } throws IllegalStateException("database closed")
        coEvery { stringResourceProvider.getString(Res.string.sms_shortcut_error) } returns "error"

        val reply = registerSmsFromShortcut("purchase")

        assertFalse(reply.isRegistered)
        assertEquals("error", reply.message)
    }
}
