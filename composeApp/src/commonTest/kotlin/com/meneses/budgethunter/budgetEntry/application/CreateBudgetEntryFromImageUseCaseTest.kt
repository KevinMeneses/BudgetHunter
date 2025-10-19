package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.ImageData
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CreateBudgetEntryFromImageUseCaseTest {

    @Test
    fun `merges ai response with existing budget entry preserving non blank values`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val original = BudgetEntry(
            amount = "125.00",
            description = "Grocery run",
            category = BudgetEntry.Category.GROCERIES,
            date = "2024-01-15"
        )
        val aiEntry = original.copy(
            amount = "240.50",
            description = "",
            category = BudgetEntry.Category.HEALTH,
            date = ""
        )
        var capturedImageData: ImageData? = null
        val processor = RecordingAIProcessor(aiEntry) { imageData, _ ->
            capturedImageData = imageData
        }
        val useCase = CreateBudgetEntryFromImageUseCase(processor, dispatcher)

        val result = useCase.execute("invoice.jpg", original)

        assertFalse(capturedImageData!!.isPdf, "JPG should not be flagged as PDF")
        assertEquals("240.50", result.amount)
        assertEquals("Grocery run", result.description, "Blank AI description keeps original value")
        assertEquals(BudgetEntry.Category.HEALTH, result.category)
        assertEquals("2024-01-15", result.date, "Blank AI date keeps original value")
    }

    @Test
    fun `returns original budget entry when processor fails or throws`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val original = BudgetEntry(description = "Dinner", category = BudgetEntry.Category.FOOD)
        val failingProcessor = RecordingAIProcessor(result = null) { imageData, prompt ->
            assertTrue(imageData.isPdf, "PDF extension should be detected ignoring case")
            assertTrue(prompt.contains("receipt", ignoreCase = true))
        }
        val throwingProcessor = object : AIImageProcessor {
            override suspend fun processImage(imageData: ImageData, prompt: String): BudgetEntry? {
                throw IllegalStateException("boom")
            }
        }
        val useCase = CreateBudgetEntryFromImageUseCase(failingProcessor, dispatcher)
        val throwingUseCase = CreateBudgetEntryFromImageUseCase(throwingProcessor, dispatcher)

        val nullResult = useCase.execute("receipt.PDF", original)
        val exceptionResult = throwingUseCase.execute("receipt.png", original)

        assertSame(original, nullResult)
        assertSame(original, exceptionResult)
    }

    private class RecordingAIProcessor(
        private val result: BudgetEntry?,
        private val onCall: (ImageData, String) -> Unit = { _, _ -> }
    ) : AIImageProcessor {
        override suspend fun processImage(imageData: ImageData, prompt: String): BudgetEntry? {
            onCall(imageData, prompt)
            return result
        }
    }
}
