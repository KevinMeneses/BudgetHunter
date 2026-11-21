package com.meneses.budgethunter.sms.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegisteredMovementsTest {

    private val dataStore = InMemoryPreferencesDataStore()

    @Test
    fun `a fingerprint is new only the first time`() = runTest {
        val registeredMovements = RegisteredMovements(dataStore)

        assertTrue(registeredMovements.markIfNew("a1"))
        assertFalse(registeredMovements.markIfNew("a1"))
        assertTrue(registeredMovements.markIfNew("b2"))
    }

    @Test
    fun `what was registered is remembered after the process restarts`() = runTest {
        RegisteredMovements(dataStore).markIfNew("a1")

        assertFalse(RegisteredMovements(dataStore).markIfNew("a1"))
    }

    @Test
    fun `two copies handled at once let only one through`() = runTest {
        val registeredMovements = RegisteredMovements(dataStore)

        val results = List(10) { async { registeredMovements.markIfNew("a1") } }.awaitAll()

        assertEquals(1, results.count { it })
    }
}
