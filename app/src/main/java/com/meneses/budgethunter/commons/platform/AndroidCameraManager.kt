package com.meneses.budgethunter.commons.platform

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.meneses.budgethunter.commons.data.FileData
import com.meneses.budgethunter.commons.data.toFileData
import java.io.File

interface CameraLauncherDelegate {
    fun launchCamera(uri: Uri)
}

class AndroidCameraManager(
    private val context: Context
) : CameraManager {

    private var currentPhotoUri: Uri? = null
    private var currentCallback: ((FileData?) -> Unit)? = null
    private var launcherDelegate: CameraLauncherDelegate? = null

    fun setLauncherDelegate(delegate: CameraLauncherDelegate) {
        this.launcherDelegate = delegate
    }

    override fun takePhoto(onResult: (FileData?) -> Unit) {
        currentCallback = onResult

        val photoFile = File(context.filesDir, "temp_invoice_picture_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )

        launcherDelegate?.launchCamera(currentPhotoUri!!)
    }

    fun handlePhotoResult(success: Boolean) {
        val callback = currentCallback ?: return
        currentCallback = null

        if (success && currentPhotoUri != null) {
            try {
                val fileData = currentPhotoUri!!.toFileData(context.contentResolver, context.filesDir)
                callback(fileData)
            } catch (e: Exception) {
                callback(null)
            }
        } else {
            callback(null)
        }

        currentPhotoUri = null
    }
}
