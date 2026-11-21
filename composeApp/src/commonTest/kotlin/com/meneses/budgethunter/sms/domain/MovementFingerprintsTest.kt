package com.meneses.budgethunter.sms.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class MovementFingerprintsTest {

    private val payment = BudgetEntry(
        budgetId = 7,
        amount = "130500.00",
        description = "Bancolombia: Pagaste \$130,500.00 a EMPRESA DE ENERGIA desde tu producto 1234 el 20/11/2026 21:14:58",
        type = BudgetEntry.Type.OUTCOME,
        date = "2026-11-20"
    )

    private fun fingerprint(
        message: String = MESSAGE,
        entry: BudgetEntry = payment,
        receivedAt: Long? = RECEIVED_AT
    ) = MovementFingerprints.ofEmailMovement(SupportedBanks.BANCOLOMBIA, message, entry, receivedAt)

    @Test
    fun `the same email movement gives the same fingerprint wherever it was read`() {
        assertEquals(fingerprint(), fingerprint(entry = payment.copy(budgetId = 9, id = 4)))
    }

    @Test
    fun `email movements told apart by their time or amount give different fingerprints`() {
        assertNotEquals(fingerprint(), fingerprint(message = MESSAGE.replace("21:14:58", "21:20:03")))
        assertNotEquals(fingerprint(), fingerprint(entry = payment.copy(amount = "1000")))
    }

    @Test
    fun `a time written after the first sentence still tells two movements apart`() {
        val morning = "Notificación. Realizaste una compra por \$35.900 en TIENDA D1. Hora 08:11"
        val evening = "Notificación. Realizaste una compra por \$35.900 en TIENDA D1. Hora 19:40"
        val sameDescription = payment.copy(description = "Davivienda: Realizaste una compra por \$35.900 en TIENDA D1")

        assertNotEquals(
            fingerprint(message = morning, entry = sameDescription),
            fingerprint(message = evening, entry = sameDescription)
        )
    }

    @Test
    fun `an email posted again on a later day is the same movement`() {
        assertEquals(fingerprint(), fingerprint(entry = payment.copy(date = "2026-11-21")))
    }

    @Test
    fun `the same text in emails of different days is two movements`() {
        val oneDay = 24 * 60 * 60 * 1000L

        assertNotEquals(fingerprint(), fingerprint(receivedAt = RECEIVED_AT + oneDay))
    }

    @Test
    fun `an email with no arrival time is told by the day it was read`() {
        assertEquals(fingerprint(receivedAt = null), fingerprint(receivedAt = 0L))
        assertNotEquals(
            fingerprint(receivedAt = null),
            fingerprint(entry = payment.copy(date = "2026-11-21"), receivedAt = null)
        )
    }

    @Test
    fun `an app notification is told by its identity`() {
        assertEquals(
            MovementFingerprints.ofAppNotification("0|com.nu|1|null|10|1795227302131"),
            MovementFingerprints.ofAppNotification("0|com.nu|1|null|10|1795227302131")
        )
        assertNotEquals(
            MovementFingerprints.ofAppNotification("0|com.nu|1|null|10|1795227302131"),
            MovementFingerprints.ofAppNotification("0|com.nu|1|null|10|1795227399999")
        )
    }

    @Test
    fun `a fingerprint already remembered is not added again`() {
        val stored = MovementFingerprints.remember(null, "a1")

        assertEquals("a1", stored)
        assertNull(MovementFingerprints.remember(stored, "a1"))
        assertEquals("a1,b2", MovementFingerprints.remember(stored, "b2"))
    }

    @Test
    fun `the oldest fingerprints are forgotten past the capacity`() {
        var stored: String? = null
        for (fingerprint in listOf("a1", "b2", "c3")) {
            stored = MovementFingerprints.remember(stored, fingerprint, capacity = 2)
        }

        assertEquals("b2,c3", stored)
        assertEquals("c3,a1", MovementFingerprints.remember(stored, "a1", capacity = 2))
    }

    private companion object {
        const val MESSAGE = "Alertas y Notificaciones. Bancolombia: Pagaste \$130,500.00 a EMPRESA DE ENERGIA desde " +
            "tu producto 1234 el 20/11/2026 21:14:58."

        // 20 Nov 2026 around 21:15 in Colombia, well inside the day in any time zone near it
        const val RECEIVED_AT = 1_795_227_302_131L
    }
}
