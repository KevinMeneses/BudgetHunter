package com.meneses.budgethunter.commons.platform

import com.meneses.budgethunter.commons.data.FileData

interface FilePickerManager {
    fun pickFile(
        mimeTypes: Array<String> = arrayOf("image/*", "application/pdf"),
        onResult: (FileData?) -> Unit
    )
}
