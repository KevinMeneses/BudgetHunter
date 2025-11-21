package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.data.FileData
import com.meneses.budgethunter.commons.data.IFileManager

class FakeFileManager : IFileManager {
    val savedFiles = mutableListOf<FileData>()
    val deletedFiles = mutableListOf<String>()

    override fun saveFile(fileData: FileData): String {
        savedFiles.add(fileData)
        return "/saved/file_${savedFiles.size}.pdf"
    }

    override fun deleteFile(filePath: String): Boolean {
        deletedFiles.add(filePath)
        return true
    }

    override fun createUri(filePath: String): String = "file://$filePath"

    override fun fileExists(filePath: String): Boolean = true
}
