package com.speech_studio.domain.text

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

/**
 * Locale-independent Russian date and time formatting.
 *
 * Platform formatters depend on the system locale, so the application uses this explicit
 * vocabulary instead of `DateTimeFormatter`/`TextStyle`.
 */
object RussianText {
    private val monthsGenitive = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )
    private val monthsNominative = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    private val monthsShort = listOf(
        "янв.", "февр.", "мар.", "апр.", "мая", "июн.",
        "июл.", "авг.", "сент.", "окт.", "нояб.", "дек."
    )
    private val weekdaysShort = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
    private val weekdaysFull = listOf(
        "понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"
    )

    fun monthGenitive(month: Int): String = monthsGenitive[month - 1]

    fun monthNominative(month: Int): String = monthsNominative[month - 1]

    fun monthShort(month: Int): String = monthsShort[month - 1]

    fun weekdayShort(isoDayNumber: Int): String = weekdaysShort[isoDayNumber - 1]

    fun weekdayFull(isoDayNumber: Int): String = weekdaysFull[isoDayNumber - 1]

    fun formatNumericDate(date: LocalDate): String =
        "${date.day.twoDigits()}.${date.month.number.twoDigits()}.${date.year.fourDigits()}"

    fun formatLongDate(date: LocalDate): String =
        "${date.day} ${monthGenitive(date.month.number)} ${date.year}"

    /** Дата без года для компактных плиток: `28 октября`. Год виден в заголовке календаря. */
    fun formatDayMonth(date: LocalDate): String =
        "${date.day} ${monthGenitive(date.month.number)}"

    fun formatLongDatePadded(date: LocalDate): String =
        "${date.day.twoDigits()} ${monthGenitive(date.month.number)} ${date.year}"

    /** Часовая метка сеток, например `09:00`. */
    fun formatHour(hour: Int): String = "${hour.twoDigits()}:00"

    fun formatDayHeader(date: LocalDate): String =
        "${weekdayShort(date.dayOfWeek.isoDayNumber).replaceFirstChar { it.uppercase() }}, " +
            "${date.day} ${monthShort(date.month.number)} ${date.year}"

    fun formatWeekRange(start: LocalDate, end: LocalDate): String =
        if (start.month == end.month) {
            "${start.day}–${end.day} ${monthShort(end.month.number)} ${end.year}"
        } else {
            "${start.day} ${monthShort(start.month.number)} – ${end.day} ${monthShort(end.month.number)}"
        }

    fun formatTime(time: LocalTime): String =
        "${time.hour.twoDigits()}:${time.minute.twoDigits()}"

    fun formatDateTime(dateTime: LocalDateTime): String =
        "${formatNumericDate(dateTime.date)} ${formatTime(dateTime.time)}"

    /**
     * Число с существительным в правильной форме: `1 занятие`, `2 занятия`, `5 занятий`.
     */
    fun formatCount(value: Int, one: String, few: String, many: String): String =
        "$value ${nounForm(value, one, few, many)}"

    /**
     * Существительное в форме, согласованной с числом: для `1` — [one], для `2–4` — [few],
     * для остальных и для `11–14` — [many]. Нужна там, где число показано отдельно.
     */
    fun nounForm(value: Int, one: String, few: String, many: String): String {
        val absolute = if (value < 0) -value else value
        val lastTwo = absolute % 100
        val last = absolute % 10
        return when {
            lastTwo in 11..14 -> many
            last == 1 -> one
            last in 2..4 -> few
            else -> many
        }
    }

    private fun Int.twoDigits(): String = if (this < 10) "0$this" else toString()

    private fun Int.fourDigits(): String {
        val text = toString()
        return if (text.length >= 4) text else "0".repeat(4 - text.length) + text
    }
}
