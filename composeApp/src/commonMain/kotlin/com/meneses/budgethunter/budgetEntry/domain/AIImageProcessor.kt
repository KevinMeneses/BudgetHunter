package com.meneses.budgethunter.budgetEntry.domain

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
     * @return A BudgetEntry with AI-extracted information, or null if processing fails
     */
    suspend fun processImage(imageData: ImageData, prompt: String): BudgetEntry?
}
