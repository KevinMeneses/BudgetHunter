package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
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
        private val result: BudgetEntry? = null,
        private val error: Exception? = null
    ) : AIImageProcessor {
        var imageData: ImageData? = null
        var prompt: String? = null

        override suspend fun processImage(imageData: ImageData, prompt: String): BudgetEntry? {
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

        val result = useCase(FakeProcessor(ai)).execute("file:///a.jpg", original)

        assertEquals(7, result.id)
        assertEquals(3, result.budgetId)
        assertEquals("99.5", result.amount)
        assertEquals("Market", result.description)
        assertEquals(BudgetEntry.Category.GROCERIES, result.category)
        assertEquals("2025-03-04", result.date)
    }

    @Test
    fun `null ai result keeps the original entry`() = runTest {
        val original = BudgetEntry(amount = "5", description = "mine")

        assertEquals(original, useCase(FakeProcessor(null)).execute("file:///a.jpg", original))
    }

    @Test
    fun `processor failure keeps the original entry`() = runTest {
        val original = BudgetEntry(amount = "5", description = "mine")

        val result = useCase(FakeProcessor(error = RuntimeException("boom"))).execute("file:///a.jpg", original)

        assertEquals(original, result)
    }
}
