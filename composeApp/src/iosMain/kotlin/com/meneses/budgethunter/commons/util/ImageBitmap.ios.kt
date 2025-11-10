package com.meneses.budgethunter.commons.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextDrawPDFPage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGDataProviderCreateWithData
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
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
 * iOS implementation for loading ImageBitmap from PDF file.
 * Uses iOS Core Graphics APIs to render PDF to bitmap.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun getImageBitmapFromPDFFile(filePath: String): ImageBitmap? {
    return try {
        println("iOS getImageBitmapFromPDFFile called with path: $filePath")

        // Check if file exists first
        val fileManager = NSFileManager.defaultManager
        val fileExists = fileManager.fileExistsAtPath(filePath)
        println("iOS PDF file exists: $fileExists")

        if (!fileExists) {
            println("PDF file does not exist at path: $filePath")
            return getImageBitmapFromFile(filePath)
        }

        // Use NSData and CGDataProvider for better compatibility
        val nsData = NSData.dataWithContentsOfFile(filePath)
        if (nsData == null) {
            println("Failed to read PDF file data from: $filePath")
            return null
        }

        // Create data provider from NSData using proper C interop
        val dataBytes = nsData.bytes?.reinterpret<ByteVar>()
        val dataProvider = CGDataProviderCreateWithData(
            info = null,
            data = dataBytes,
            size = nsData.length,
            releaseData = null
        )

        if (dataProvider == null) {
            println("Failed to create data provider for PDF")
            return null
        }

        val document = CGPDFDocumentCreateWithProvider(dataProvider)
        println("iOS PDF document created: ${document != null}")

        if (document == null) {
            println("Failed to load PDF document from: $filePath")
            return null // Don't fallback to getImageBitmapFromFile for PDFs
        }

        val pageCount = CGPDFDocumentGetNumberOfPages(document).toInt()
        if (pageCount < 1) {
            println("PDF document has no pages: $filePath")
            return null
        }

        val page = CGPDFDocumentGetPage(document, 1u) // Get first page
        if (page == null) {
            println("Failed to get first page from PDF: $filePath")
            return null
        }

        val pageRect = CGPDFPageGetBoxRect(page, kCGPDFMediaBox)
        val width = pageRect.useContents { size.width.toInt() }
        val height = pageRect.useContents { size.height.toInt() }

        if (width <= 0 || height <= 0) {
            println("Invalid PDF page dimensions: $width x $height")
            return null
        }

        memScoped {
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
                println("Failed to create bitmap context for PDF")
                CGColorSpaceRelease(colorSpace)
                return null
            }

            // White background
            CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
            CGContextFillRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))

            // Draw the PDF page
            CGContextDrawPDFPage(context, page)

            // Convert to ImageBitmap
            val imageRef = CGBitmapContextCreateImage(context)
            val imageBitmap = if (imageRef != null) {
                cgImageToImageBitmap(imageRef, width, height)
            } else {
                println("Failed to create CGImage from PDF context")
                null
            }

            // Cleanup
            if (imageRef != null) CGImageRelease(imageRef)
            CGContextRelease(context)
            CGColorSpaceRelease(colorSpace)

            imageBitmap
        }
    } catch (e: Exception) {
        println("Error loading PDF file '$filePath': ${e.message}")
        getImageBitmapFromFile(filePath)
    }
}

/**
 * iOS implementation for loading ImageBitmap from file path.
 * Uses UIImage and Core Graphics APIs to load and convert images.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun getImageBitmapFromFile(filePath: String): ImageBitmap? {
    return try {
        println("iOS getImageBitmapFromFile called with path: $filePath")

        // Handle different path formats
        val cleanPath = when {
            filePath.startsWith("file://") -> filePath.removePrefix("file://")
            else -> filePath
        }

        println("iOS cleaned path: $cleanPath")

        // Check if file exists
        val fileManager = NSFileManager.defaultManager
        val fileExists = fileManager.fileExistsAtPath(cleanPath)
        println("iOS file exists: $fileExists")

        if (!fileExists) {
            println("File does not exist at path: $cleanPath")
            return null
        }

        val uiImage = UIImage.imageWithContentsOfFile(cleanPath)
        if (uiImage == null) {
            println("Failed to load UIImage from: $cleanPath")
            return null
        }

        println("iOS UIImage loaded successfully, size: ${uiImage.size}")

        val cgImage = uiImage.CGImage
        if (cgImage == null) {
            println("Failed to get CGImage from UIImage: $cleanPath")
            return null
        }

        val width = CGImageGetWidth(cgImage).toInt()
        val height = CGImageGetHeight(cgImage).toInt()

        if (width <= 0 || height <= 0) {
            println("Invalid image dimensions: $width x $height")
            return null
        }

        cgImageToImageBitmap(cgImage, width, height)
    } catch (e: Exception) {
        println("Error loading image file '$filePath': ${e.message}")
        null
    }
}

/**
 * Converts a CGImage to Compose ImageBitmap using Skia.
 */
@OptIn(ExperimentalForeignApi::class)
private fun cgImageToImageBitmap(cgImage: CGImageRef, width: Int, height: Int): ImageBitmap? {
    return try {
        memScoped {
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
                println("Failed to create bitmap context")
                CGColorSpaceRelease(colorSpace)
                return null
            }

            // Draw the image into the context
            val rect = CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())
            CGContextDrawImage(context, rect, cgImage)

            // Convert bitmap data to ByteArray
            val byteArray = ByteArray(bitmapByteCount) { index ->
                bitmapData[index]
            }

            // Cleanup
            CGContextRelease(context)
            CGColorSpaceRelease(colorSpace)

            // Convert to Skia Image and then to Compose ImageBitmap
            val skiaImage = Image.makeRaster(
                imageInfo = org.jetbrains.skia.ImageInfo.makeN32Premul(width, height),
                bytes = byteArray,
                rowBytes = bytesPerRow
            )

            skiaImage.toComposeImageBitmap()
        }
    } catch (e: Exception) {
        println("Error converting CGImage to ImageBitmap: ${e.message}")
        null
    }
}
