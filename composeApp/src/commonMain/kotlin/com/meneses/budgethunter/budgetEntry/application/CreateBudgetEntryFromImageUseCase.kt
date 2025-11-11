package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.ImageData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Creates budget entries from images using AI processing.
 * Preserves all the AI prompt engineering logic from the original Android implementation
 * while providing cross-platform compatibility.
 */
class CreateBudgetEntryFromImageUseCase(
    private val aiImageProcessor: AIImageProcessor,
    private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * AI prompt preserved from the original Android implementation.
     * This maintains the exact same prompt engineering that was working in the Android app.
     */
    private val prompt = """this is the image of a receipt, obtain the following items:

            - total amount (ignore dots and only consider commas as a decimal part)
            - a high level description of what was paid (avoid adding the words receipt, invoice, bill, or document)
            - a category among these: ${BudgetEntry.getCategories()}
            - a date (if it is not available in the image, use ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date})

        present this information in the following JSON structure as an API valid JSON response
        without any additional character that would break serialization:

        { "amount": "0", "description": "something", "category": "GROCERIES", "date:"2025-04-01" }

        or return an empty response if the image is not an invoice, receipt or bill"""

    suspend fun execute(
        imageUri: String,
        budgetEntry: BudgetEntry
    ): BudgetEntry = withContext(ioDispatcher) {
        try {
            // Create image data object with proper PDF detection
            val imageData = ImageData(
                uri = imageUri,
                isPdf = imageUri.endsWith(".pdf", ignoreCase = true)
            )

            // Process the image using AI
            val aiBudgetEntry = aiImageProcessor.processImage(imageData, prompt)

            // If AI processing successful, merge with existing budget entry
            if (aiBudgetEntry != null) {
                budgetEntry.copy(
                    amount = aiBudgetEntry.amount.takeIf { it.isNotBlank() } ?: budgetEntry.amount,
                    description = aiBudgetEntry.description.takeIf { it.isNotBlank() } ?: budgetEntry.description,
                    category = aiBudgetEntry.category,
                    date = aiBudgetEntry.date.takeIf { it.isNotBlank() } ?: budgetEntry.date
                )
            } else {
                // Return original budget entry if AI processing fails
                budgetEntry
            }
        } catch (e: Exception) {
            // Log error in debug mode but return original budget entry to maintain user experience
            println("KMP AI Image Processing Error: ${e.message}")
            budgetEntry
        }
    }
}
