package com.meneses.budgethunter.commons.data

/**
 * Cross-platform file management interface for BudgetHunter.
 * Provides essential file operations for budget entry invoice handling.
 */
expect class FileManager {

    /**
     * Saves file data to the platform-appropriate directory
     * @param fileData The file data to save
     * @return The absolute path to the saved file
     */
    fun saveFile(fileData: FileData): String

    /**
     * Deletes a file at the specified path
     * @param filePath The absolute path to the file to delete
     * @return true if file was successfully deleted, false otherwise
     */
    fun deleteFile(filePath: String): Boolean

    /**
     * Creates a platform-appropriate URI for the file
     * @param filePath The absolute path to the file
     * @return URI string for the file
     */
    fun createUri(filePath: String): String

    /**
     * Validates if a file exists at the specified path
     * @param filePath The absolute path to check
     * @return true if file exists, false otherwise
     */
    fun fileExists(filePath: String): Boolean
}
