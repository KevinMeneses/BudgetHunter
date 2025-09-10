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
}
