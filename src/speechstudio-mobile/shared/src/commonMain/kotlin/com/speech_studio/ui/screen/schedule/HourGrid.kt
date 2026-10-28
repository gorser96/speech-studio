package com.speech_studio.ui.screen.schedule

import com.speech_studio.domain.text.RussianText
import kotlinx.datetime.LocalDate

private const val INITIAL_GRID_HOUR = 8

/**
 * Час, на котором открывается сетка недели или дня: 08:00, если он входит
 * в действующий диапазон настройки, иначе начало диапазона. Занятия дня
 * на начальное положение не влияют.
 */
fun initialGridScrollHour(startHour: Int, endHour: Int): Int {
    val range = effectiveGridRange(startHour, endHour)
    return if (INITIAL_GRID_HOUR in range) INITIAL_GRID_HOUR else range.first
}

/**
 * Подписи границ часов: метка каждой строки диапазона и конечная граница.
 * Для 08–21 это 13 строк и 14 меток от `08:00` до `21:00`.
 */
fun hourGridLabels(startHour: Int, endHour: Int): List<String> {
    val range = effectiveGridRange(startHour, endHour)
    return (range.first..(range.last + 1)).map { RussianText.formatHour(it) }
}

/** Последний видимый день недели: пятница при скрытых выходных, иначе воскресенье. */
fun visibleWeekEnd(weekStart: LocalDate, weekendHidden: Boolean): LocalDate =
    weekStart.plusDays(if (weekendHidden) 4 else 6)
