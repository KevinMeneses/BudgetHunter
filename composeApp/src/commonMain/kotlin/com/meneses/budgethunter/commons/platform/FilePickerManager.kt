package com.meneses.budgethunter.commons.platform

import com.meneses.budgethunter.commons.data.FileData

expect class FilePickerManager {
    fun pickFile(
        mimeTypes: Array<String> = arrayOf("image/*", "application/pdf"),
        onResult: (FileData?) -> Unit
    )
}