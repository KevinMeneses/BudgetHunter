package com.meneses.budgethunter.commons.util

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.Foundation.stringByRemovingPercentEncoding
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSMutableCharacterSet

@OptIn(BetaInteropApi::class)
actual fun encodeURL(string: String, encoding: String): String {
    return when (encoding.uppercase()) {
        "UTF-8", "UTF_8" -> {
            val nsString = NSString.create(string = string)
            // Create a character set that matches Java's URLEncoder behavior
            // Java URLEncoder encodes everything except: alphanumeric, *, -, ., _
            val allowedCharacterSet = NSMutableCharacterSet()
            allowedCharacterSet.formUnionWithCharacterSet(NSCharacterSet.alphanumericCharacterSet())
            allowedCharacterSet.addCharactersInString("*-._")

            nsString.stringByAddingPercentEncodingWithAllowedCharacters(
                allowedCharacterSet
            ) ?: string
        }
        else -> {
            // Fallback for other encodings - just encode with UTF-8
            val nsString = NSString.create(string = string)
            val allowedCharacterSet = NSMutableCharacterSet()
            allowedCharacterSet.formUnionWithCharacterSet(NSCharacterSet.alphanumericCharacterSet())
            allowedCharacterSet.addCharactersInString("*-._")

            nsString.stringByAddingPercentEncodingWithAllowedCharacters(
                allowedCharacterSet
            ) ?: string
        }
    }
}

@OptIn(BetaInteropApi::class)
actual fun decodeURL(string: String, encoding: String): String {
    return when (encoding.uppercase()) {
        "UTF-8", "UTF_8" -> {
            val nsString = NSString.create(string = string)
            nsString.stringByRemovingPercentEncoding ?: string
        }
        else -> {
            // Fallback for other encodings
            val nsString = NSString.create(string = string)
            nsString.stringByRemovingPercentEncoding ?: string
        }
    }
}
