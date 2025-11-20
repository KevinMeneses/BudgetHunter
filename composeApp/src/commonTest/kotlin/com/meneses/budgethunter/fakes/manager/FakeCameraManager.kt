package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.platform.CameraManager

class FakeCameraManager : CameraManager {
    var callback: ((ByteArray?) -> Unit)? = null

    override fun takePhoto(onResult: (ByteArray?) -> Unit) {
        callback = onResult
    }

    fun simulatePhotoTaken(data: ByteArray?) {
        callback?.invoke(data)
    }
}
