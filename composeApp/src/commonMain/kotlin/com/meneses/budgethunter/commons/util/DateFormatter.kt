package com.meneses.budgethunter.commons.util

import kotlinx.datetime.toLocalDateTime

fun formatDateForDisplay(dateString: String?): String {
    if (dateString.isNullOrBlank()) return ""
    
    return try {
        val parts = dateString.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val month = parts[1].padStart(2, '0')
            val day = parts[2].padStart(2, '0')
            "$day/$month/$year"
        } else {
            dateString
        }
    } catch (_: Exception) {
        dateString
    }
}

fun getCurrentDate(): String {
    val now = kotlinx.datetime.Clock.System.now()
    val localDateTime = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val year = localDateTime.year
    val month = localDateTime.monthNumber
    val day = localDateTime.dayOfMonth
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}
