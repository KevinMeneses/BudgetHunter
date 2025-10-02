package com.meneses.budgethunter.budgetEntry.domain

import android.graphics.Bitmap
import android.util.Base64
import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import com.meneses.budgethunter.budgetEntry.data.remote.GeminiApiClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Android-specific implementation of AIImageProcessor.
 * Converts Android Bitmap to base64 and delegates to shared GeminiApiClient.
 */
class AndroidAIImageProcessor(
    private val geminiApiClient: GeminiApiClient,
    private val imageProcessor: ImageProcessor,
    private val ioDispatcher: CoroutineDispatcher
) : AIImageProcessor {

    override suspend fun processImage(imageData: ImageData, prompt: String): BudgetEntry? = withContext(ioDispatcher) {
        try {
            // Get the bitmap from the image URI
            val bitmap = imageProcessor.getImageFromUri(imageData) as? Bitmap
                ?: return@withContext null

            // Convert Bitmap to base64-encoded JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val imageBytes = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            // Use shared Gemini API client
            geminiApiClient.extractBudgetEntryFromImage(base64Image, prompt)
        } catch (e: Exception) {
            println("Android AI Image Processing Error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
