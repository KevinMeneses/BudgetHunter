package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.data.FileData
import com.meneses.budgethunter.commons.platform.CameraManager

class FakeCameraManager : CameraManager {
    var callback: ((FileData?) -> Unit)? = null

    override fun takePhoto(onResult: (FileData?) -> Unit) {
        callback = onResult
    }

    fun simulatePhotoTaken(data: ByteArray?) {
        val fileData = data?.let {
            FileData(
                data = it,
                filename = "photo.jpg",
                mimeType = "image/jpeg",
                directory = "/tmp"
            )
        }
        callback?.invoke(fileData)
    }
}
