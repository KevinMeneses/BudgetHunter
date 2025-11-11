package com.meneses.budgethunter.budgetEntry.domain

/**
 * Common representation of image data across platforms.
 * Contains the necessary information to process an image for AI analysis.
 */
data class ImageData(
    val uri: String,
    val isPdf: Boolean = false
) {
    /**
     * Determines if this image data represents a PDF file based on the URI extension.
     */
    fun isPdfFile(): Boolean = uri.endsWith(".pdf", ignoreCase = true) || isPdf
}
