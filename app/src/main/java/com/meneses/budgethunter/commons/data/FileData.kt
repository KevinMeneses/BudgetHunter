package com.meneses.budgethunter.commons.data

data class FileData(
    val data: ByteArray,
    val filename: String,
    val mimeType: String? = null,
    val directory: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileData

        if (!data.contentEquals(other.data)) return false
        if (filename != other.filename) return false
        if (mimeType != other.mimeType) return false
        if (directory != other.directory) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + directory.hashCode()
        return result
    }
}