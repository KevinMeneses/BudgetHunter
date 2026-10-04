package com.meneses.budgethunter.sms.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import com.meneses.budgethunter.sms.domain.SmsParseResult
import com.meneses.budgethunter.sms.domain.SupportedBanks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SmsMapperTest {

    private val preferencesManager = mockk<PreferencesManager>()
    private val budgetLocalDataSource = mockk<BudgetLocalDataSource>()
    private val stringResourceProvider = mockk<StringResourceProvider>(relaxed = true)
    private val bankConfig = SupportedBanks.BANCOLOMBIA

    private val mapper = SmsMapper(preferencesManager, budgetLocalDataSource, stringResourceProvider)

    private val purchaseSms =
        "Bancolombia le informa Compra por $50.000 en EXITO POBLADO desde su cuenta *1234."

    @Test
    fun `smsToBudgetEntry returns Success when the default budget exists`() = runTest {
        coEvery { preferencesManager.getDefaultBudgetId() } returns 7
        coEvery { budgetLocalDataSource.getById(7) } returns Budget(id = 7, name = "Default", amount = 100.0)

        val result = mapper.smsToBudgetEntry(purchaseSms, bankConfig)

        assertIs<SmsParseResult.Success>(result)
        assertEquals(7, result.entry.budgetId)
        assertEquals("50000", result.entry.amount)
        assertEquals(BudgetEntry.Type.OUTCOME, result.entry.type)
    }

    @Test
    fun `smsToBudgetEntry returns NoDefaultBudget when the default budget no longer exists`() = runTest {
        coEvery { preferencesManager.getDefaultBudgetId() } returns 7
        coEvery { budgetLocalDataSource.getById(7) } returns null

        val result = mapper.smsToBudgetEntry(purchaseSms, bankConfig)

        assertEquals(SmsParseResult.NoDefaultBudget, result)
    }

    @Test
    fun `smsToBudgetEntry returns NoDefaultBudget without lookup when the default id is not positive`() = runTest {
        coEvery { preferencesManager.getDefaultBudgetId() } returns 0

        val result = mapper.smsToBudgetEntry(purchaseSms, bankConfig)

        assertEquals(SmsParseResult.NoDefaultBudget, result)
        coVerify(exactly = 0) { budgetLocalDataSource.getById(any()) }
    }

    @Test
    fun `smsToBudgetEntry returns Unrecognized for an unrelated message regardless of budget`() = runTest {
        coEvery { preferencesManager.getDefaultBudgetId() } returns 7
        coEvery { budgetLocalDataSource.getById(7) } returns null

        val result = mapper.smsToBudgetEntry("Hola, nos vemos manana", bankConfig)

        assertEquals(SmsParseResult.Unrecognized, result)
    }
}
