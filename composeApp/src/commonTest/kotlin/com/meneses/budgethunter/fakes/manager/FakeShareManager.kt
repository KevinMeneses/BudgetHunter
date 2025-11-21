package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.platform.ShareManager

class FakeShareManager : ShareManager {
    val sharedFiles = mutableListOf<String>()

    override fun shareFile(filePath: String, mimeTypes: Array<String>) {
        sharedFiles.add(filePath)
    }
}
