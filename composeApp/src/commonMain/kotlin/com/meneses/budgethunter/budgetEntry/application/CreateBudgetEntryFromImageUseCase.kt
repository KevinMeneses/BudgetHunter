package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.AIImageProcessor
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.ImageData
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Creates budget entries from images using AI processing.
 * The prompt is built per call (so "today" is current) and the response is constrained
 * by a structured-output schema in GeminiApiClient.
 */
class CreateBudgetEntryFromImageUseCase(
    private val aiImageProcessor: AIImageProcessor,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) {
    private val tag = "CreateBudgetEntryFromImageUseCase"

    private fun buildPrompt(today: LocalDate) = """
        You are extracting data from a photo or scan of a receipt, invoice or bill to register an expense.
        The document may be in any language and may be a photo with glare, skew or low contrast. Read it carefully.

        If the image is not a receipt, invoice or bill, set "isInvoice" to false and leave the rest empty.

        Otherwise extract:

        - "amount": the FINAL TOTAL actually paid (the amount due after taxes, discounts and tip).
          Do not return the subtotal, a tax line, a change/cash-tendered amount or a single line item.
          Return a plain number: use a dot as decimal separator and no thousands separators or currency symbols.
          Decide how to read separators from the document's currency and locale
          (e.g. "1.234,50" and "1,234.50" both mean 1234.5; "12.500" in a currency without cents means 12500).
        - "description": a short, high level description of what was paid (for example the merchant or the kind
          of purchase). Do not include the words receipt, invoice, bill or document. Use the document's language.
        - "category": exactly one of the following values:
          ${categoryGuide()}
          If none clearly applies, use OTHER.
        - "date": the date of the purchase in YYYY-MM-DD format. Resolve ambiguous formats using the document's
          locale (day-first is common outside the US). If no date is visible, use $today.

        Respond only with a JSON object matching the provided schema.
    """.trimIndent()

    private fun categoryGuide() = BudgetEntry.getCategories().joinToString("\n          ") { category ->
        "- ${category.name}: ${categoryHints.getValue(category)}"
    }

    private val categoryHints = mapOf(
        BudgetEntry.Category.FOOD to "restaurants, cafes, delivery, bars",
        BudgetEntry.Category.GROCERIES to "supermarket and food/drink shopping to cook at home",
        BudgetEntry.Category.SELF_CARE to "hair, beauty, cosmetics, gym, personal care",
        BudgetEntry.Category.TRANSPORTATION to "fuel, public transport, taxi/ride-hailing, parking, tolls, car maintenance",
        BudgetEntry.Category.HOUSEHOLD_ITEMS to "furniture, appliances, cleaning supplies, hardware, home goods",
        BudgetEntry.Category.SERVICES to "utilities, internet, phone, subscriptions, rent, professional services",
        BudgetEntry.Category.EDUCATION to "tuition, courses, books, school supplies",
        BudgetEntry.Category.HEALTH to "pharmacy, doctors, insurance, medical tests",
        BudgetEntry.Category.LEISURE to "entertainment, travel, events, hobbies, streaming",
        BudgetEntry.Category.TAXES to "taxes, fines, government fees",
        BudgetEntry.Category.OTHER to "anything that does not fit the categories above"
    )

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
            val aiBudgetEntry = aiImageProcessor.processImage(
                imageData,
                buildPrompt(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
            )

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
            // Return original budget entry to maintain user experience
            logger.warn(tag, "AI image processing error", e)
            budgetEntry
        }
    }
}
