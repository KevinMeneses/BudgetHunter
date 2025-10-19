package com.meneses.budgethunter.sms.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class SmsMapperTest {

    private lateinit var dataStore: InMemoryPreferencesDataStore
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var mapper: SmsMapper

    @BeforeTest
    fun setUp() {
        dataStore = InMemoryPreferencesDataStore()
        preferencesManager = PreferencesManager(dataStore)
        mapper = SmsMapper(preferencesManager)
    }

    @Test
    fun `returns budget entry when sms matches configuration`() = runTest {
        preferencesManager.setDefaultBudgetId(7)
        val config = BankSmsConfig(
            id = "bank",
            displayName = "BankPlus",
            senderKeywords = listOf("BankPlus"),
            transactionAmountRegex = Regex("compra por \\$?([\\d.,]+)", RegexOption.IGNORE_CASE),
            transactionDescriptionRegex = Regex("en ([A-Za-z ]+)", RegexOption.IGNORE_CASE)
        )
        val message = "Aviso BankPlus: Compra por $1.234,56 en Super Store"

        val result = mapper.smsToBudgetEntry(message, config)

        assertNotNull(result)
        assertEquals("1234.56", result.amount)
        assertEquals("Super Store", result.description)
        assertEquals(BudgetEntry.Type.OUTCOME, result.type)
        assertEquals(7, result.budgetId)
    }

    @Test
    fun `returns null when default budget is missing`() = runTest {
        preferencesManager.setDefaultBudgetId(-1)
        val config = BankSmsConfig(id = "bank", displayName = "BankPlus", senderKeywords = listOf("BankPlus"))

        val result = mapper.smsToBudgetEntry("Aviso BankPlus: Compra por $100 en tienda", config)

        assertNull(result)
    }

    @Test
    fun `returns null when sms lacks bank keyword`() = runTest {
        preferencesManager.setDefaultBudgetId(3)
        val config = BankSmsConfig(id = "bank", displayName = "BankPlus", senderKeywords = listOf("BankPlus"))

        val result = mapper.smsToBudgetEntry("Aviso OtroBanco: Compra por $100", config)

        assertNull(result)
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val mutex = Mutex()
        private var preferences = emptyPreferences()
        private val state = MutableStateFlow(preferences)

        override val data: Flow<Preferences>
            get() = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            mutex.withLock {
                val newPrefs = transform(preferences)
                preferences = newPrefs
                state.value = newPrefs
                newPrefs
            }
    }
}
