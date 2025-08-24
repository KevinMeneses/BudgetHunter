package com.meneses.budgethunter.budgetEntry.application

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.util.getBitmapFromPDFFileDescriptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate

class GetAIBudgetEntryFromImageUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json,
    private val generativeModel: GenerativeModel
) {
    private val prompt =
        """this is the image of a receipt, obtain the following items:

            - total amount (ignore dots and only consider commas as a decimal part)
            - a high level description of what was paid (avoid adding the words receipt, invoice, bill, or document)
            - a category among these: ${BudgetEntry.getCategories()}
            - a date (if it is not available in the image, use ${LocalDate.now()})

        present this information in the following JSON structure as an API valid JSON response
        without any additional character that would break serialization:

        { "amount": "0", "description": "something", "category": "GROCERIES", "date:"2025-04-01" }

        or return an empty response if the image is not an invoice, receipt or bill"""

    suspend fun execute(
        imageUri: Uri,
        budgetEntry: BudgetEntry,
        contentResolver: ContentResolver
    ): BudgetEntry = withContext(ioDispatcher) {
        try {
            val bitmap = when {
                imageUri.path?.endsWith(".pdf") == true -> {
                    val descriptor = contentResolver.openFileDescriptor(
                        /* uri = */ imageUri,
                        /* mode = */ "r"
                    )!!
                    getBitmapFromPDFFileDescriptor(descriptor)
                }

                else -> {
                    contentResolver
                        .openInputStream(imageUri)
                        ?.use(BitmapFactory::decodeStream)!!
                }
            }

            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val refinedText = response.text!!
                .removePrefix("```json\n")
                .removeSuffix("```")

            val aiBudgetEntry = json.decodeFromString<BudgetEntry>(refinedText)

            budgetEntry.copy(
                amount = aiBudgetEntry.amount,
                description = aiBudgetEntry.description,
                category = aiBudgetEntry.category,
                date = aiBudgetEntry.date
            )
        } catch (e: Exception) {
            budgetEntry
        }
    }
}
