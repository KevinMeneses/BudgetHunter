package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.ImageData
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawPDFPage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGDataProviderCreateWithData
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGPDFDocumentCreateWithProvider
import platform.CoreGraphics.CGPDFDocumentGetNumberOfPages
import platform.CoreGraphics.CGPDFDocumentGetPage
import platform.CoreGraphics.CGPDFPageGetBoxRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGPDFMediaBox
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile
import platform.UIKit.UIImage

/**
 * iOS-specific implementation of ImageProcessor.
 * Handles UIImage loading from file URIs with proper orientation handling.
 */
actual class ImageProcessor {

    @OptIn(ExperimentalForeignApi::class)
    actual fun getImageFromUri(imageData: ImageData): Any? {
        return try {
            if (imageData.isPdfFile()) {
                processPdfImage(imageData)
            } else {
                processRegularImage(imageData)
            }
        } catch (e: Exception) {
            println("iOS Image Processing Error: ${e.message}")
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun processPdfImage(imageData: ImageData): UIImage? {
        return try {
            val cleanPath = cleanFilePath(imageData.uri)

            // Verify file exists
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(cleanPath)) {
                println("iOS PDF Processing: File not found at $cleanPath")
                return null
            }

            // Load PDF data
            val nsData = NSData.dataWithContentsOfFile(cleanPath)
            if (nsData == null) {
                println("iOS PDF Processing: Failed to read PDF data from $cleanPath")
                return null
            }

            // Create data provider from NSData
            val dataBytes = nsData.bytes?.reinterpret<ByteVar>()
            val dataProvider = CGDataProviderCreateWithData(
                info = null,
                data = dataBytes,
                size = nsData.length,
                releaseData = null
            )

            if (dataProvider == null) {
                println("iOS PDF Processing: Failed to create data provider")
                return null
            }

            // Create PDF document
            val document = CGPDFDocumentCreateWithProvider(dataProvider)
            if (document == null) {
                println("iOS PDF Processing: Failed to create PDF document")
                return null
            }

            val pageCount = CGPDFDocumentGetNumberOfPages(document).toInt()
            if (pageCount < 1) {
                println("iOS PDF Processing: PDF has no pages")
                return null
            }

            // Get first page
            val page = CGPDFDocumentGetPage(document, 1u)
            if (page == null) {
                println("iOS PDF Processing: Failed to get first page")
                return null
            }

            // Get page dimensions
            val pageRect = CGPDFPageGetBoxRect(page, kCGPDFMediaBox)
            val width = pageRect.useContents { size.width.toInt() }
            val height = pageRect.useContents { size.height.toInt() }

            if (width <= 0 || height <= 0) {
                println("iOS PDF Processing: Invalid page dimensions: $width x $height")
                return null
            }

            // Render PDF to UIImage
            val uiImage = memScoped {
                val colorSpace = CGColorSpaceCreateDeviceRGB()
                val bytesPerPixel = 4
                val bytesPerRow = bytesPerPixel * width
                val bitmapByteCount = bytesPerRow * height

                val bitmapData = allocArray<ByteVar>(bitmapByteCount)
                val context = CGBitmapContextCreate(
                    bitmapData,
                    width.toULong(),
                    height.toULong(),
                    8u,
                    bytesPerRow.toULong(),
                    colorSpace,
                    CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                )

                if (context == null) {
                    println("iOS PDF Processing: Failed to create bitmap context")
                    CGColorSpaceRelease(colorSpace)
                    return null
                }

                // White background
                CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
                CGContextFillRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))

                // Draw PDF page
                CGContextDrawPDFPage(context, page)

                // Create CGImage from context
                val cgImage = CGBitmapContextCreateImage(context)

                // Cleanup
                CGContextRelease(context)
                CGColorSpaceRelease(colorSpace)

                if (cgImage == null) {
                    println("iOS PDF Processing: Failed to create CGImage")
                    null
                } else {
                    // Convert CGImage to UIImage
                    val resultImage = UIImage.imageWithCGImage(cgImage)
                    println("iOS PDF Processing: Successfully rendered PDF to UIImage")
                    resultImage
                }
            }

            uiImage
        } catch (e: Exception) {
            println("iOS PDF Processing Error: ${e.message}")
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun processRegularImage(imageData: ImageData): UIImage? {
        return try {
            val cleanPath = cleanFilePath(imageData.uri)

            // Verify file exists
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(cleanPath)) {
                println("iOS Image Processing: File not found at $cleanPath")
                return null
            }

            // Load UIImage from file path
            // UIImage automatically handles orientation via its imageOrientation property
            // No manual correction needed - the orientation is preserved for rendering
            val uiImage = UIImage.imageWithContentsOfFile(cleanPath)
            if (uiImage == null) {
                println("iOS Image Processing: Failed to load UIImage from $cleanPath")
                return null
            }

            println("iOS Image Processing: Successfully loaded image from $cleanPath")
            uiImage
        } catch (e: Exception) {
            println("iOS Regular Image Processing Error: ${e.message}")
            null
        }
    }

    /**
     * Cleans file path by removing file:// prefix and handling different path formats.
     */
    private fun cleanFilePath(uri: String): String {
        return when {
            uri.startsWith("file://") -> uri.removePrefix("file://")
            else -> uri
        }
    }
}
