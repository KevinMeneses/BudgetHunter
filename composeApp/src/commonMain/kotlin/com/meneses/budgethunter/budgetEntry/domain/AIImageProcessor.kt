package com.meneses.budgethunter.budgetEntry.domain

/**
 * Largest PDF (raw bytes) sent as-is to the AI. The request is limited to 20 MB and Base64
 * inflates the content by a third; bigger files fall back to rendering the first page.
 */
const val MAX_INLINE_PDF_BYTES = 12 * 1024 * 1024

/**
 * Common interface for AI-powered image processing across platforms.
 * Provides abstraction for extracting budget entry information from images.
 */
interface AIImageProcessor {
    /**
     * Processes an image and returns an AI-extracted budget entry.
     *
     * @param imageData The image data to process
     * @param prompt The AI prompt to use for extraction
     * @return The extracted entry, or the reason why nothing could be extracted
     */
    suspend fun processImage(imageData: ImageData, prompt: String): AiExtractionResult
}
