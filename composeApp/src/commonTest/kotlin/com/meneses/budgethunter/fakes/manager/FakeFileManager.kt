package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.data.IFileManager

class FakeFileManager : IFileManager {
    val savedFiles = mutableListOf<ByteArray>()
    val deletedFiles = mutableListOf<String>()

    override suspend fun saveFile(fileData: ByteArray): String {
        savedFiles.add(fileData)
        return "/saved/file_${savedFiles.size}.pdf"
    }

    override fun deleteFile(filePath: String) {
        deletedFiles.add(filePath)
    }

    override fun createUri(filePath: String): String = "file://$filePath"
}
