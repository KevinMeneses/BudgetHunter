package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.platform.ShareManager

class FakeShareManager : ShareManager {
    val sharedFiles = mutableListOf<String>()

    override suspend fun shareFile(filePath: String) {
        sharedFiles.add(filePath)
    }
}
