package ru.dayplanner.app.ui

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Форматирование дат по-русски. */
object Fmt {
    private val ru: Locale = Locale.forLanguageTag("ru")
    private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", ru)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMMM yyyy", ru)
    private val weekdayFmt = DateTimeFormatter.ofPattern("EEEE", ru)
    private val monthYear = DateTimeFormatter.ofPattern("LLLL yyyy", ru)
    private val dayMonthShort = DateTimeFormatter.ofPattern("d MMM", ru)

    /** «27 ноября»; год добавляется, если он не текущий. */
    fun dayTitle(day: Long, today: Long): String {
        val date = LocalDate.ofEpochDay(day)
        return if (date.year == LocalDate.ofEpochDay(today).year) date.format(dayMonth) else date.format(dayMonthYear)
    }

    fun fullDate(day: Long): String = LocalDate.ofEpochDay(day).format(dayMonthYear)

    fun shortDate(day: Long): String = LocalDate.ofEpochDay(day).format(dayMonthShort)

    fun weekday(day: Long): String = LocalDate.ofEpochDay(day).format(weekdayFmt)

    fun monthTitle(month: YearMonth): String =
        month.format(monthYear).replaceFirstChar { it.titlecase(ru) }
}
