package com.meneses.budgethunter.commons.platform

interface ShareManager {
    fun shareFile(filePath: String, mimeTypes: Array<String> = arrayOf("application/pdf", "image/*"))

    /**
     * Opens the file in an external viewer so the user can pick the app (Android) or in the
     * system preview (iOS).
     *
     * @return false if nothing could open the file, so the caller can tell the user
     */
    fun openFile(filePath: String, mimeType: String): Boolean
}
