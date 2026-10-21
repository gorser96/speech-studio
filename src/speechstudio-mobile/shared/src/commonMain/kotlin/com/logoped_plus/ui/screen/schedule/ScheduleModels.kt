package com.logoped_plus.ui.screen.schedule

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class ScheduleViewMode {
    MONTH,
    WEEK,
    DAY
}

fun LocalDate.startOfWeek(): LocalDate = minus(dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

fun LocalDate.daysInMonth(): Int = plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day

fun LocalDate.atStartOfDay(): LocalDateTime = atTime(0, 0)

fun LocalDate.plusDays(days: Int): LocalDate = plus(days, DateTimeUnit.DAY)

fun LocalDate.minusDays(days: Int): LocalDate = minus(days, DateTimeUnit.DAY)

fun LocalDate.plusWeeks(weeks: Int): LocalDate = plus(weeks, DateTimeUnit.WEEK)

fun LocalDate.minusWeeks(weeks: Int): LocalDate = minus(weeks, DateTimeUnit.WEEK)

fun LocalDate.plusMonths(months: Int): LocalDate = plus(months, DateTimeUnit.MONTH)

fun LocalDate.minusMonths(months: Int): LocalDate = minus(months, DateTimeUnit.MONTH)

fun LocalDateTime.plusDays(days: Int): LocalDateTime = plusMinutes(days * MINUTES_PER_DAY)

fun LocalDateTime.plusMinutes(minutes: Int): LocalDateTime {
    val total = date.toEpochDays() * NANOS_PER_DAY +
        time.toNanosecondOfDay() +
        minutes * NANOS_PER_MINUTE
    val day = total.floorDiv(NANOS_PER_DAY)
    val nanos = total.mod(NANOS_PER_DAY)
    return LocalDate.fromEpochDays(day).atTime(LocalTime.fromNanosecondOfDay(nanos))
}

private const val MINUTES_PER_DAY = 24 * 60
private const val NANOS_PER_MINUTE = 60_000_000_000L
private const val NANOS_PER_DAY = 86_400_000_000_000L

fun effectiveGridRange(startHour: Int, endHour: Int): IntRange {
    return if (startHour in 0..23 && endHour in startHour + 1..24) startHour until endHour else 0..23
}
