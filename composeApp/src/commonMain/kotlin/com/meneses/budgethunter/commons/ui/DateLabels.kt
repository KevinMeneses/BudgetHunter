package com.meneses.budgethunter.commons.ui

import androidx.compose.runtime.Composable
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.day_header_format
import budgethunter.composeapp.generated.resources.day_header_format_year
import budgethunter.composeapp.generated.resources.month_abbreviations
import budgethunter.composeapp.generated.resources.short_date_format
import budgethunter.composeapp.generated.resources.today
import budgethunter.composeapp.generated.resources.weekday_abbreviations
import budgethunter.composeapp.generated.resources.yesterday
import com.meneses.budgethunter.commons.util.RelativeDay
import com.meneses.budgethunter.commons.util.currentDate
import com.meneses.budgethunter.commons.util.relativeTo
import com.meneses.budgethunter.commons.util.toLocalDateOrNull
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/**
 * Header for the entries of one day: "Hoy", "Ayer", "Sáb 3 oct", and the year only when it is
 * not the current one. Falls back to the stored text when the date does not parse.
 */
@Composable
fun dayHeaderLabel(date: String, today: LocalDate = currentDate()): String {
    val parsed = date.toLocalDateOrNull() ?: return date
    when (parsed.relativeTo(today)) {
        RelativeDay.TODAY -> return stringResource(Res.string.today)
        RelativeDay.YESTERDAY -> return stringResource(Res.string.yesterday)
        RelativeDay.OTHER -> Unit
    }

    // Both arrays start on Monday / January, as kotlinx-datetime numbers them
    val weekday = stringArrayResource(Res.array.weekday_abbreviations)[parsed.dayOfWeek.ordinal]
    val month = stringArrayResource(Res.array.month_abbreviations)[parsed.monthNumber - 1]
    return if (parsed.year == today.year) {
        stringResource(Res.string.day_header_format, weekday, parsed.dayOfMonth, month)
    } else {
        stringResource(Res.string.day_header_format_year, weekday, parsed.dayOfMonth, month, parsed.year)
    }
}

/** A stored date as "3 oct 2026" / "Oct 3, 2026". */
@Composable
fun shortDateLabel(date: String): String {
    val parsed = date.toLocalDateOrNull() ?: return date
    val month = stringArrayResource(Res.array.month_abbreviations)[parsed.monthNumber - 1]
    return stringResource(Res.string.short_date_format, parsed.dayOfMonth, month, parsed.year)
}
