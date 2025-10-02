package com.meneses.budgethunter.commons.platform

import com.meneses.budgethunter.commons.data.FileData

interface CameraManager {
    fun takePhoto(onResult: (FileData?) -> Unit)
}
