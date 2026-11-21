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

    @Test
    fun `the same email movement gives the same fingerprint wherever it was read`() {
        assertEquals(
            MovementFingerprints.ofEmailMovement(SupportedBanks.BANCOLOMBIA, payment),
            MovementFingerprints.ofEmailMovement(SupportedBanks.BANCOLOMBIA, payment.copy(budgetId = 9, id = 4))
        )
    }

    @Test
    fun `email movements told apart by their time or amount give different fingerprints`() {
        val fingerprint = MovementFingerprints.ofEmailMovement(SupportedBanks.BANCOLOMBIA, payment)

        val later = payment.copy(description = payment.description.replace("21:14:58", "21:20:03"))
        assertNotEquals(fingerprint, MovementFingerprints.ofEmailMovement(SupportedBanks.BANCOLOMBIA, later))
        assertNotEquals(fingerprint, MovementFingerprints.ofEmailMovement(SupportedBanks.BANCOLOMBIA, payment.copy(amount = "1000")))
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
}
