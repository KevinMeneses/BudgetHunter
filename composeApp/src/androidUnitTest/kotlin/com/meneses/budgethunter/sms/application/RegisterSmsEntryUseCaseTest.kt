package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import com.meneses.budgethunter.sms.data.InMemoryPreferencesDataStore
import com.meneses.budgethunter.sms.data.RegisteredMovements
import com.meneses.budgethunter.sms.data.SmsMapper
import com.meneses.budgethunter.sms.domain.BankEmail
import com.meneses.budgethunter.sms.domain.BankNotification
import com.meneses.budgethunter.sms.domain.SmsParseResult
import com.meneses.budgethunter.sms.domain.SupportedBanks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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

    // Stands for the preferences file, which outlives the process
    private val preferencesFile = InMemoryPreferencesDataStore()

    private val registerSmsEntry = newRegisterSmsEntry()

    /** A new one over the same file is what a new process gets. */
    private fun newRegisterSmsEntry() = RegisterSmsEntryUseCase(
        smsMapper = SmsMapper(preferencesManager, budgetLocalDataSource, stringResourceProvider),
        budgetEntryRepository = budgetEntryRepository,
        registeredMovements = RegisteredMovements(preferencesFile)
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

    @Test
    fun `registers a Nu notification described by its text after the bank name`() = runTest {
        givenDefaultBudget()
        val stored = slot<BudgetEntry>()
        coEvery { budgetEntryRepository.create(capture(stored)) } returns Unit

        val result = registerSmsEntry.registerNotification(
            BankNotification(
                title = "Enviaste $1.952,00",
                text = "Le enviaste a KEV**** MEN****** en su cuenta de Bancolombia."
            ),
            SupportedBanks.NU
        )

        assertIs<SmsParseResult.Success>(result)
        assertEquals("1952.00", stored.captured.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, stored.captured.type)
        assertEquals("Nu: Le enviaste a KEV**** MEN****** en su cuenta de Bancolombia", stored.captured.description)
        assertEquals(BudgetEntry.Category.UNCATEGORIZED, stored.captured.category)
    }

    @Test
    fun `does not store a notification that reports no movement`() = runTest {
        givenDefaultBudget()

        val result = registerSmsEntry.registerNotification(
            BankNotification(title = "Nequi", text = "Ya puedes usar tu nueva tarjeta en cualquier comercio"),
            SupportedBanks.NEQUI
        )

        assertIs<SmsParseResult.Unrecognized>(result)
        coVerify(exactly = 0) { budgetEntryRepository.create(any()) }
    }

    @Test
    fun `registers a bank email described by its body`() = runTest {
        givenDefaultBudget()
        val stored = slot<BudgetEntry>()
        coEvery { budgetEntryRepository.createLocally(capture(stored)) } returns Unit
        val email = BankEmail.fromNotification(
            title = "Davivienda",
            text = "Notificación de transacción",
            bigText = "Notificación de transacción\nApreciado cliente: realizaste una compra por \$35.900 en " +
                "TIENDA D1. Si no la reconoces llámanos."
        )

        val result = registerSmsEntry.registerEmail(email, SupportedBanks.DAVIVIENDA)

        assertIs<SmsParseResult.Success>(result)
        assertEquals("35900", stored.captured.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, stored.captured.type)
        assertEquals(
            "Davivienda: Apreciado cliente: realizaste una compra por \$35.900 en TIENDA D1",
            stored.captured.description
        )
    }

    private fun bancolombiaEmail(time: String = "21:14:58") = BankEmail.fromNotification(
        title = "alertasynotificaciones",
        text = "Alertas y Notificaciones",
        bigText = "Alertas y Notificaciones\n \nBancolombia: Pagaste \$130,500.00 a EMPRESA DE ENERGIA desde tu " +
            "producto 1234 el 20/11/2026 $time. ¿Dudas? Llamanos al 6045109095. Estamos cerca\n" +
            "Por tu seguridad, desde Bancolombia nunca te solicitaremos tus datos personales, como usuario y clave; " +
            "ni tus datos bancarios, como números de tarjetas de crédito, códigos de seguridad y plazos.",
        receivedAt = RECEIVED_AT
    )

    @Test
    fun `registers a bank email only once when the email app posts it again`() = runTest {
        givenDefaultBudget()

        val first = registerSmsEntry.registerEmail(bancolombiaEmail(), SupportedBanks.BANCOLOMBIA)
        val again = registerSmsEntry.registerEmail(bancolombiaEmail(), SupportedBanks.BANCOLOMBIA)

        assertIs<SmsParseResult.Success>(first)
        assertEquals(SmsParseResult.Ignored, again)
        coVerify(exactly = 1) { budgetEntryRepository.createLocally(any()) }
    }

    @Test
    fun `does not register a bank email again after the process restarted`() = runTest {
        givenDefaultBudget()
        registerSmsEntry.registerEmail(bancolombiaEmail(), SupportedBanks.BANCOLOMBIA)

        val afterRestart = newRegisterSmsEntry().registerEmail(bancolombiaEmail(), SupportedBanks.BANCOLOMBIA)

        assertEquals(SmsParseResult.Ignored, afterRestart)
        coVerify(exactly = 1) { budgetEntryRepository.createLocally(any()) }
    }

    @Test
    fun `registers two bank emails of movements at different times`() = runTest {
        givenDefaultBudget()

        registerSmsEntry.registerEmail(bancolombiaEmail(time = "21:14:58"), SupportedBanks.BANCOLOMBIA)
        val second = registerSmsEntry.registerEmail(bancolombiaEmail(time = "21:20:03"), SupportedBanks.BANCOLOMBIA)

        assertIs<SmsParseResult.Success>(second)
        coVerify(exactly = 2) { budgetEntryRepository.createLocally(any()) }
    }

    @Test
    fun `an email with no default budget is not taken as registered`() = runTest {
        coEvery { preferencesManager.getDefaultBudgetId() } returns -1
        registerSmsEntry.registerEmail(bancolombiaEmail(), SupportedBanks.BANCOLOMBIA)
        givenDefaultBudget()

        val result = registerSmsEntry.registerEmail(bancolombiaEmail(), SupportedBanks.BANCOLOMBIA)

        assertIs<SmsParseResult.Success>(result)
    }

    @Test
    fun `a bank email whose entry could not be stored is tried again`() = runTest {
        givenDefaultBudget()
        var attempts = 0
        coEvery { budgetEntryRepository.createLocally(any()) } answers {
            attempts++
            if (attempts == 1) error("database unavailable")
        }

        runCatching { registerSmsEntry.registerEmail(bancolombiaEmail(), SupportedBanks.BANCOLOMBIA) }
        val retried = registerSmsEntry.registerEmail(bancolombiaEmail(), SupportedBanks.BANCOLOMBIA)

        assertIs<SmsParseResult.Success>(retried)
        assertEquals(2, attempts)
    }

    private val nuTransfer = BankNotification(
        title = "Enviaste $1.952,00",
        text = "Le enviaste a KEV**** MEN****** en su cuenta de Bancolombia."
    )

    @Test
    fun `registers a bank app notification once by its identity also after the process restarted`() = runTest {
        givenDefaultBudget()

        val first = registerSmsEntry.registerNotification(nuTransfer, SupportedBanks.NU, identity = "nu-key|1795227302131")
        val afterRestart = newRegisterSmsEntry().registerNotification(nuTransfer, SupportedBanks.NU, identity = "nu-key|1795227302131")
        val anotherTransfer = registerSmsEntry.registerNotification(nuTransfer, SupportedBanks.NU, identity = "nu-key|1795227399999")

        assertIs<SmsParseResult.Success>(first)
        assertEquals(SmsParseResult.Ignored, afterRestart)
        assertIs<SmsParseResult.Success>(anotherTransfer)
        coVerify(exactly = 2) { budgetEntryRepository.createLocally(any()) }
    }

    @Test
    fun `a bank app notification that reports no movement does not keep a later copy from being read`() = runTest {
        coEvery { preferencesManager.getDefaultBudgetId() } returns -1
        registerSmsEntry.registerNotification(nuTransfer, SupportedBanks.NU, identity = "nu-key|1795227302131")
        givenDefaultBudget()

        val result = registerSmsEntry.registerNotification(nuTransfer, SupportedBanks.NU, identity = "nu-key|1795227302131")

        assertIs<SmsParseResult.Success>(result)
    }

    @Test
    fun `stores a bank email entry before pushing it`() = runTest {
        givenDefaultBudget()

        registerSmsEntry.registerEmail(bancolombiaEmail(), SupportedBanks.BANCOLOMBIA)

        coVerifyOrder {
            budgetEntryRepository.createLocally(any())
            budgetEntryRepository.pushPending(7)
        }
    }

    @Test
    fun `registers two bank emails whose time comes after the first sentence`() = runTest {
        givenDefaultBudget()
        fun purchaseAt(time: String) = BankEmail.fromNotification(
            title = "Davivienda",
            text = "Notificación de transacción",
            bigText = "Notificación de transacción\nApreciado cliente: realizaste una compra por \$35.900 en " +
                "TIENDA D1. Hora: $time. Si no la reconoces llámanos.",
            receivedAt = RECEIVED_AT
        )

        val morning = registerSmsEntry.registerEmail(purchaseAt("08:11"), SupportedBanks.DAVIVIENDA)
        val evening = registerSmsEntry.registerEmail(purchaseAt("19:40"), SupportedBanks.DAVIVIENDA)

        assertIs<SmsParseResult.Success>(morning)
        assertIs<SmsParseResult.Success>(evening)
        coVerify(exactly = 2) { budgetEntryRepository.createLocally(any()) }
    }

    private companion object {
        const val RECEIVED_AT = 1_795_227_302_131L
    }
}
