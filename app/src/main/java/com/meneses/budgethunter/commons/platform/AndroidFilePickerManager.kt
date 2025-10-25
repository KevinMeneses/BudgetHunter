package com.meneses.budgethunter.commons.platform

import android.content.Context
import android.net.Uri
import com.meneses.budgethunter.commons.data.FileData
import com.meneses.budgethunter.commons.data.toFileData

interface FilePickerLauncherDelegate {
    fun launchFilePicker(mimeTypes: Array<String>)
}

class AndroidFilePickerManager(
    private val context: Context
) : FilePickerManager {

    private var currentCallback: ((FileData?) -> Unit)? = null
    private var launcherDelegate: FilePickerLauncherDelegate? = null

    fun setLauncherDelegate(delegate: FilePickerLauncherDelegate) {
        this.launcherDelegate = delegate
    }

    override fun pickFile(mimeTypes: Array<String>, onResult: (FileData?) -> Unit) {
        currentCallback = onResult
        launcherDelegate?.launchFilePicker(mimeTypes)
    }

    fun handleFileResult(uri: Uri?) {
        val callback = currentCallback ?: return
        currentCallback = null

        if (uri != null) {
            try {
                val fileData = uri.toFileData(context.contentResolver, context.filesDir)
                callback(fileData)
            } catch (e: Exception) {
                callback(null)
            }
        } else {
            callback(null)
        }
    }
}
