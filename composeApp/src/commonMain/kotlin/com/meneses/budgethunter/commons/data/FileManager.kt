package com.meneses.budgethunter.commons.data

expect class FileManager {
    fun saveFile(fileData: FileData): String
    fun deleteFile(filePath: String)
    fun createUri(filePath: String): String
}