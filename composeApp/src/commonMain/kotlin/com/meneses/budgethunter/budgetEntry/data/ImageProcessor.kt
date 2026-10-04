package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.ImageData

/**
 * Platform-specific image processing capabilities.
 * Handles the conversion of image URIs to platform-specific bitmap/image objects.
 */
expect class ImageProcessor {
    /**
     * Converts image URI to platform-specific image representation.
     *
     * @param imageData The image data containing URI and metadata
     * @return Platform-specific image object (e.g., Android Bitmap, iOS UIImage)
     */
    fun getImageFromUri(imageData: ImageData): Any?

    /**
     * Reads the file behind the URI as-is (no decoding or re-encoding) and returns it Base64-encoded.
     * Used to send PDFs natively to the AI so every page is analysed at full fidelity.
     *
     * @param maxBytes Maximum raw size to accept
     * @return The Base64 content, or null if the file is unreadable, empty or larger than [maxBytes]
     */
    fun readFileAsBase64(imageData: ImageData, maxBytes: Int): String?
}
