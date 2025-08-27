package com.meneses.budgethunter.commons.data

interface FileManager {
    fun saveFile(fileData: FileData): String
    fun deleteFile(path: String): Boolean
    fun createUri(filePath: String): String
}
