package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import com.meneses.budgethunter.sms.data.SmsMapper
import com.meneses.budgethunter.sms.domain.SmsParseResult
import com.meneses.budgethunter.sms.domain.SupportedBanks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Runs the real mapper and parser, only the storage is mocked, over messages written like the
 * ones each bank sends.
 */
class RegisterSmsEntryUseCaseTest {

    private val preferencesManager = mockk<PreferencesManager>()
    private val budgetLocalDataSource = mockk<BudgetLocalDataSource>()
    private val stringResourceProvider = mockk<StringResourceProvider>(relaxed = true)
    private val budgetEntryRepository = mockk<BudgetEntryRepository>(relaxed = true)

    private val registerSmsEntry = RegisterSmsEntryUseCase(
        smsMapper = SmsMapper(preferencesManager, budgetLocalDataSource, stringResourceProvider),
        budgetEntryRepository = budgetEntryRepository
    )

    private fun givenDefaultBudget(id: Int = 7) {
        coEvery { preferencesManager.getDefaultBudgetId() } returns id
        coEvery { budgetLocalDataSource.getById(id) } returns Budget(id = id, name = "Hogar", amount = 1_000_000.0)
    }

    private suspend fun register(message: String) = registerSmsEntry(message, SupportedBanks.ALL_BANKS)

    private suspend fun assertRegistered(message: String, amount: String, type: BudgetEntry.Type) {
        val stored = slot<BudgetEntry>()
        coEvery { budgetEntryRepository.create(capture(stored)) } returns Unit

        val result = register(message)

        assertIs<SmsParseResult.Success>(result)
        assertEquals(amount, stored.captured.amount)
        assertEquals(type, stored.captured.type)
        assertEquals(7, stored.captured.budgetId)
        assertEquals(BudgetEntry.Category.UNCATEGORIZED, stored.captured.category)
    }

    @Test
    fun `registers a Bancolombia debit purchase`() = runTest {
        givenDefaultBudget()

        assertRegistered(
            "Bancolombia: Compraste COP25.000,00 en EXITO CALLE 80 con tu T.Deb *1234, el 26/10/2026 " +
                "a las 10:12. Si tienes dudas, encuentranos aqui: 6045109095. Estamos cerca.",
            amount = "25000.00",
            type = BudgetEntry.Type.OUTCOME
        )
    }

    @Test
    fun `registers a Bancolombia Bre-B income`() = runTest {
        givenDefaultBudget()

        assertRegistered(
            "Bancolombia: Recibiste $120.000 por Bre-B de JUAN PEREZ en tu cuenta *1234.",
            amount = "120000",
            type = BudgetEntry.Type.INCOME
        )
    }

    @Test
    fun `registers the purchases of other supported banks`() = runTest {
        givenDefaultBudget()

        assertRegistered(
            "Davivienda le informa compra aprobada por $45.900 en RAPPI COLOMBIA con su tarjeta terminada en 4321.",
            amount = "45900",
            type = BudgetEntry.Type.OUTCOME
        )
        assertRegistered(
            "BBVA: Realizaste una compra por $120.000,00 en FALABELLA UNICENTRO con tu tarjeta *5678 el 26/10/2026.",
            amount = "120000.00",
            type = BudgetEntry.Type.OUTCOME
        )
        assertRegistered(
            "Scotiabank Colpatria: Pago de $89.900 a CLARO COLOMBIA aprobado desde tu cuenta *3456.",
            amount = "89900",
            type = BudgetEntry.Type.OUTCOME
        )
        assertRegistered(
            "AV Villas: Compra por $15.500 en DROGUERIA ALEMANA tarjeta *1122.",
            amount = "15500",
            type = BudgetEntry.Type.OUTCOME
        )
        assertRegistered(
            "BANCO BOGOTA: Retiro por $200.000 en SERVIBANCA CENTRO desde tu cuenta *9012.",
            amount = "200000",
            type = BudgetEntry.Type.OUTCOME
        )
    }

    @Test
    fun `registers an income reported by Banco Popular`() = runTest {
        givenDefaultBudget()

        assertRegistered(
            "Bco Popular: Transferencia recibida por $350.000 de CARLOS RUIZ en su cuenta *7890.",
            amount = "350000",
            type = BudgetEntry.Type.INCOME
        )
    }

    @Test
    fun `reads a message that names no bank with the first configuration`() = runTest {
        givenDefaultBudget()

        assertRegistered(
            "Compra por $9.900 en TIENDA D1 con tu tarjeta *1234.",
            amount = "9900",
            type = BudgetEntry.Type.OUTCOME
        )
    }

    @Test
    fun `stores nothing for advertising`() = runTest {
        givenDefaultBudget()

        val result = register("Bancolombia: Felicitaciones, tienes un cupo preaprobado de $5.000.000. Solicitalo ya en la app.")

        assertEquals(SmsParseResult.Ignored, result)
        coVerify(exactly = 0) { budgetEntryRepository.create(any()) }
    }

    @Test
    fun `stores nothing for a message it cannot read`() = runTest {
        givenDefaultBudget()

        val result = register("Bancolombia: Se realizo una operacion por $75.000 en tu cuenta de ahorros.")

        assertEquals(SmsParseResult.Unrecognized, result)
        coVerify(exactly = 0) { budgetEntryRepository.create(any()) }
    }

    @Test
    fun `tells there is no default budget before telling the message was unreadable`() = runTest {
        coEvery { preferencesManager.getDefaultBudgetId() } returns -1

        val result = register("Bancolombia: Compraste $25.000 en EXITO con tu T.Deb *1234.")

        assertEquals(SmsParseResult.NoDefaultBudget, result)
        coVerify(exactly = 0) { budgetEntryRepository.create(any()) }
    }

    @Test
    fun `ignores a message when there are no banks to read it with`() = runTest {
        givenDefaultBudget()

        val result = registerSmsEntry("Bancolombia: Compraste $25.000 en EXITO.", emptyList())

        assertEquals(SmsParseResult.Ignored, result)
        coVerify(exactly = 0) { budgetEntryRepository.create(any()) }
    }
}
