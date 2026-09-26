package com.meneses.budgethunter.commons.util

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Today in the device time zone, in the "yyyy-MM-dd" form budgets and entries store.
 */
fun today(): String =
    Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()

/**
 * Calendar date of a backend timestamp: "2026-09-10T21:48:21.123456" -> "2026-09-10".
 * Used to recover the date of records the server stored before it kept a date of its own.
 */
fun String.toCalendarDate(): String? =
    substringBefore('T').takeIf { date ->
        runCatching { LocalDate.parse(date) }.isSuccess
    }
