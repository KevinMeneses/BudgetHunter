package com.meneses.budgethunter.budgetEntry.domain

import com.meneses.budgethunter.budgetEntry.data.ImageProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * iOS-specific implementation of AIImageProcessor.
 * Currently provides placeholder implementations for future iOS AI development.
 * When iOS AI capabilities are added, this will integrate with iOS ML frameworks
 * such as CoreML, Vision, or third-party AI services.
 */
class IosAIImageProcessor(
    private val imageProcessor: ImageProcessor,
    private val ioDispatcher: CoroutineDispatcher
) : AIImageProcessor {

    override suspend fun processImage(imageData: ImageData, prompt: String): BudgetEntry? = withContext(ioDispatcher) {
        // TODO: Implement iOS AI image processing
        // Future implementation options:
        // 1. CoreML with custom trained model
        // 2. Vision framework for text recognition + custom parsing
        // 3. Third-party AI services (OpenAI, Anthropic, etc.)
        // 4. On-device OCR + rule-based extraction
        
        println("iOS AI Image Processing: Not yet implemented")
        println("Image URI: ${imageData.uri}")
        println("Prompt: $prompt")
        
        // Return null to indicate processing is not available
        // The common use case will fall back to returning the original budget entry
        null
    }
}
