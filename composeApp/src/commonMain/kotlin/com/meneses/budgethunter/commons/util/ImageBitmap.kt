package com.meneses.budgethunter.commons.util

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Cross-platform function to load ImageBitmap from file path.
 * Used for displaying attached invoices in budget entry modal.
 */
expect fun getImageBitmapFromFile(filePath: String): ImageBitmap?

/**
 * Cross-platform function to load ImageBitmap from PDF file.
 * Falls back to regular image loading if PDF rendering fails.
 */
expect fun getImageBitmapFromPDFFile(filePath: String): ImageBitmap?