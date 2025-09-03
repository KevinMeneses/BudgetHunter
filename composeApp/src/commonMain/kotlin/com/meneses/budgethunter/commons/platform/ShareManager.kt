package com.meneses.budgethunter.commons.platform

expect class ShareManager {
    fun shareFile(filePath: String, mimeTypes: Array<String> = arrayOf("application/pdf", "image/*"))
}