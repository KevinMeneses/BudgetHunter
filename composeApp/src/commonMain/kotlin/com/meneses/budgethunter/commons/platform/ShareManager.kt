package com.meneses.budgethunter.commons.platform

interface ShareManager {
    fun shareFile(filePath: String, mimeTypes: Array<String> = arrayOf("application/pdf", "image/*"))
}