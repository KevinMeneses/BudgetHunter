package com.meneses.budgethunter.budgetEntry.domain

import kotlinx.datetime.LocalDate

/**
 * Largest PDF (raw bytes) sent as-is to the AI. The server accepts up to 12 MB (16 MB once Base64
 * inflates it by a third); bigger files fall back to rendering the first page.
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
     * @param today The user's local date, used when the document shows none
     * @return The extracted entry, or the reason why nothing could be extracted
     */
    suspend fun processImage(imageData: ImageData, today: LocalDate): AiExtractionResult
}
