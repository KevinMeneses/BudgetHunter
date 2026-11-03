package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.AiExtractionResult
import com.meneses.budgethunter.budgetEntry.domain.AiFailureReason
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.ImageData
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * Creates budget entries from images using AI processing.
 * The file goes to the BudgetHunter server, which holds the prompt and the AI key and counts the
 * scan against the account's plan; "today" is sent along for documents without a date.
 */
class CreateBudgetEntryFromImageUseCase(
    private val aiImageProcessor: AIImageProcessor,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) {
    private val tag = "CreateBudgetEntryFromImageUseCase"

    /**
     * @property entry The entry to use: the original one with the AI-extracted fields merged in,
     * or the original untouched when nothing could be extracted.
     * @property failure Why nothing was extracted, or null when the AI result was applied.
     */
    data class Result(val entry: BudgetEntry, val failure: AiFailureReason? = null)

    suspend fun execute(
        imageUri: String,
        budgetEntry: BudgetEntry
    ): Result = withContext(ioDispatcher) {
        try {
            val imageData = ImageData(
                uri = imageUri,
                isPdf = imageUri.endsWith(".pdf", ignoreCase = true)
            )

            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            when (val result = aiImageProcessor.processImage(imageData, today)) {
                is AiExtractionResult.Success -> Result(merge(budgetEntry, result.entry))
                // Keep what the user already has and let the caller explain what went wrong
                is AiExtractionResult.Failure -> Result(budgetEntry, result.reason)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(tag, "AI image processing error", e)
            Result(budgetEntry, AiFailureReason.UNKNOWN)
        }
    }

    private fun merge(original: BudgetEntry, ai: BudgetEntry) = original.copy(
        // Ignore amounts that are not a positive number instead of filling the form with garbage
        amount = ai.amount.trim().takeIf { it.toDoubleOrNull()?.let { value -> value > 0 } == true }
            ?: original.amount,
        description = ai.description.takeIf { it.isNotBlank() } ?: original.description,
        // OTHER is the "no idea" answer: don't overwrite a category the user already picked with it.
        // On an entry still waiting for one this keeps it waiting ("Sin categoría"), so the server
        // can try from the description later; any other category the receipt gives is a real choice
        // and the server never touches it.
        category = ai.category.takeIf { it != BudgetEntry.Category.OTHER } ?: original.category,
        date = ai.date.takeIf { it.isNotBlank() } ?: original.date
    )
}
