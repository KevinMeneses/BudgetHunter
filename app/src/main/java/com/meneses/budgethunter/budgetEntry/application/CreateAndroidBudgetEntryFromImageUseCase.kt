package com.meneses.budgethunter.budgetEntry.application

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.util.getBitmapFromPDFFileDescriptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate

class CreateAndroidBudgetEntryFromImageUseCase(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher,
    private val generativeModel: GenerativeModel,
    private val json: Json
) : CreateBudgetEntryFromImageUseCase {

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

    override suspend fun execute(
        imageUri: String,
        budgetEntry: BudgetEntry
    ): BudgetEntry = withContext(ioDispatcher) {
        try {
            val bitmap = getBitmapFromUri(imageUri)

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

    private fun getBitmapFromUri(imageUri: String): Bitmap {
        val uri = imageUri.toUri()
        return when {
            uri.path?.endsWith(".pdf") == true -> {
                getBitmapFromPdfUri(imageUri)
            }
            else -> {
                contentResolver
                    .openInputStream(uri)
                    ?.use(BitmapFactory::decodeStream)!!
            }
        }
    }

    private fun getBitmapFromPdfUri(pdfUri: String): Bitmap {
        val uri = pdfUri.toUri()
        val descriptor = contentResolver.openFileDescriptor(uri, "r")!!
        return getBitmapFromPDFFileDescriptor(descriptor)
    }
}
