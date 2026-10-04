package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.AiExtractionResult
import com.meneses.budgethunter.budgetEntry.domain.AiFailureReason
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.ImageData
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateBudgetEntryFromImageUseCaseTest {

    private class FakeProcessor(
        private val result: AiExtractionResult = AiExtractionResult.Failure(AiFailureReason.NOT_AN_INVOICE),
        private val error: Exception? = null
    ) : AIImageProcessor {
        var imageData: ImageData? = null
        var prompt: String? = null

        override suspend fun processImage(imageData: ImageData, prompt: String): AiExtractionResult {
            this.imageData = imageData
            this.prompt = prompt
            error?.let { throw it }
            return result
        }
    }

    private fun useCase(processor: AIImageProcessor) =
        CreateBudgetEntryFromImageUseCase(processor, Dispatchers.Unconfined, NoOpLogger())

    @Test
    fun `prompt lists every category, today's date and no broken JSON example`() = runTest {
        val processor = FakeProcessor()

        useCase(processor).execute("file:///a.jpg", BudgetEntry())

        val prompt = processor.prompt.orEmpty()
        BudgetEntry.Category.entries.forEach { assertTrue(prompt.contains("- ${it.name}:"), it.name) }
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        assertTrue(prompt.contains(today.toString()))
        assertTrue(prompt.contains("FINAL TOTAL"))
        assertFalse(prompt.contains("\"date:\""))
    }

    @Test
    fun `pdf uri is flagged as pdf`() = runTest {
        val processor = FakeProcessor()

        useCase(processor).execute("file:///invoice.PDF", BudgetEntry())

        assertTrue(processor.imageData!!.isPdf)
    }

    @Test
    fun `ai result is merged into the entry`() = runTest {
        val ai = BudgetEntry(
            amount = "99.5",
            description = "Market",
            category = BudgetEntry.Category.GROCERIES,
            date = "2025-03-04"
        )
        val original = BudgetEntry(id = 7, budgetId = 3, amount = "1", description = "old")

        val result = useCase(FakeProcessor(AiExtractionResult.Success(ai))).execute("file:///a.jpg", original)

        assertEquals(null, result.failure)
        assertEquals(7, result.entry.id)
        assertEquals(3, result.entry.budgetId)
        assertEquals("99.5", result.entry.amount)
        assertEquals("Market", result.entry.description)
        assertEquals(BudgetEntry.Category.GROCERIES, result.entry.category)
        assertEquals("2025-03-04", result.entry.date)
    }

    @Test
    fun `invalid amount keeps the user's amount`() = runTest {
        val original = BudgetEntry(amount = "5")

        listOf("abc", "0", "-3", "").forEach { badAmount ->
            val ai = BudgetEntry(amount = badAmount, description = "x")
            val result = useCase(FakeProcessor(AiExtractionResult.Success(ai))).execute("file:///a.jpg", original)
            assertEquals("5", result.entry.amount, "amount '$badAmount'")
        }
    }

    @Test
    fun `OTHER from the ai does not overwrite the user's category`() = runTest {
        val original = BudgetEntry(category = BudgetEntry.Category.HEALTH)
        val ai = BudgetEntry(amount = "10", description = "x", category = BudgetEntry.Category.OTHER)

        val result = useCase(FakeProcessor(AiExtractionResult.Success(ai))).execute("file:///a.jpg", original)

        assertEquals(BudgetEntry.Category.HEALTH, result.entry.category)
    }

    @Test
    fun `ai failure keeps the original entry and reports the reason`() = runTest {
        val original = BudgetEntry(amount = "5", description = "mine")

        val result = useCase(FakeProcessor(AiExtractionResult.Failure(AiFailureReason.NETWORK)))
            .execute("file:///a.jpg", original)

        assertEquals(original, result.entry)
        assertEquals(AiFailureReason.NETWORK, result.failure)
    }

    @Test
    fun `processor exception keeps the original entry and reports UNKNOWN`() = runTest {
        val original = BudgetEntry(amount = "5", description = "mine")

        val result = useCase(FakeProcessor(error = RuntimeException("boom"))).execute("file:///a.jpg", original)

        assertEquals(original, result.entry)
        assertEquals(AiFailureReason.UNKNOWN, result.failure)
    }
}
