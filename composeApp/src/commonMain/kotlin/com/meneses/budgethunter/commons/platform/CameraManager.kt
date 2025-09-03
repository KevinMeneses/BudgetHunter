package com.meneses.budgethunter.commons.platform

import com.meneses.budgethunter.commons.data.FileData

expect class CameraManager {
    fun takePhoto(onResult: (FileData?) -> Unit)
}