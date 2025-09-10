package com.meneses.budgethunter.budgetEntry.domain

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Android-specific implementation of AIImageProcessor using Google Generative AI.
 * This preserves all the AI logic from the original Android implementation.
 */
class AndroidAIImageProcessor(
    private val generativeModel: GenerativeModel,
    private val imageProcessor: ImageProcessor,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher
) : AIImageProcessor {

    override suspend fun processImage(imageData: ImageData, prompt: String): BudgetEntry? = withContext(ioDispatcher) {
        try {
            // Get the bitmap from the image URI
            val bitmap = imageProcessor.getImageFromUri(imageData) as? Bitmap
                ?: return@withContext null

            // Generate content using Google AI
            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val responseText = response.text ?: return@withContext null

            // Clean up the response text (preserve original logic)
            val refinedText = responseText
                .removePrefix("```json\n")
                .removeSuffix("```")
                .trim()

            // Parse the JSON response
            if (refinedText.isNotBlank() && refinedText != "{}") {
                json.decodeFromString<BudgetEntry>(refinedText)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Android AI Image Processing Error: ${e.message}")
            null
        }
    }
}
