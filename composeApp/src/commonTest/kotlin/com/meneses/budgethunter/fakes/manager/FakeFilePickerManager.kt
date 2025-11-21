package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.data.FileData
import com.meneses.budgethunter.commons.platform.FilePickerManager

class FakeFilePickerManager : FilePickerManager {
    var callback: ((FileData?) -> Unit)? = null

    override fun pickFile(mimeTypes: Array<String>, onResult: (FileData?) -> Unit) {
        callback = onResult
    }

    fun simulateFilePicked(data: ByteArray?) {
        val fileData = data?.let {
            FileData(
                data = it,
                filename = "document.pdf",
                mimeType = "application/pdf",
                directory = "/tmp"
            )
        }
        callback?.invoke(fileData)
    }
}
