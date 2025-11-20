package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.platform.FilePickerManager

class FakeFilePickerManager : FilePickerManager {
    var callback: ((ByteArray?) -> Unit)? = null

    override fun pickFile(onResult: (ByteArray?) -> Unit) {
        callback = onResult
    }

    fun simulateFilePicked(data: ByteArray?) {
        callback?.invoke(data)
    }
}
