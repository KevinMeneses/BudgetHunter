package com.meneses.budgethunter.sms.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegisteredMovementsTest {

    private val dataStore = InMemoryPreferencesDataStore()
    private var registrations = 0
    private val register: suspend () -> Unit = {
        // Gives way between reading what is remembered and remembering this one, like storing an entry does
        yield()
        registrations++
    }

    @Test
    fun `a movement is registered only the first time`() = runTest {
        val registeredMovements = RegisteredMovements(dataStore)

        assertTrue(registeredMovements.registerOnce("a1", register))
        assertFalse(registeredMovements.registerOnce("a1", register))
        assertTrue(registeredMovements.registerOnce("b2", register))
        assertEquals(2, registrations)
    }

    @Test
    fun `what was registered is remembered after the process restarts`() = runTest {
        RegisteredMovements(dataStore).registerOnce("a1", register)

        assertFalse(RegisteredMovements(dataStore).registerOnce("a1", register))
        assertEquals(1, registrations)
    }

    @Test
    fun `two copies handled at once register only one`() = runTest {
        val registeredMovements = RegisteredMovements(dataStore)

        val results = List(10) { async { registeredMovements.registerOnce("a1", register) } }.awaitAll()

        assertEquals(1, results.count { it })
        assertEquals(1, registrations)
    }

    @Test
    fun `a registration that fails is not remembered so the next copy tries again`() = runTest {
        val registeredMovements = RegisteredMovements(dataStore)

        assertFailsWith<IllegalStateException> {
            registeredMovements.registerOnce("a1") { error("database unavailable") }
        }

        assertTrue(registeredMovements.registerOnce("a1", register))
        assertEquals(1, registrations)
    }

    @Test
    fun `a movement registered but not remembered still counts as registered`() = runTest {
        dataStore.failWrites = true

        assertTrue(RegisteredMovements(dataStore).registerOnce("a1", register))
        assertEquals(1, registrations)
    }
}
