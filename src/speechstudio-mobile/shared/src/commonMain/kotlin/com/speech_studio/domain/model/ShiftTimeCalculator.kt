package com.speech_studio.domain.model

import kotlinx.datetime.LocalTime

/**
 * Вычисленная сетка смены: начала слотов, свободное время до конца окна и признак выхода за окно.
 */
data class ShiftGeometry(
    val starts: List<LocalTime>,
    val freeMinutes: Int,
    val overflows: Boolean
) {
    companion object {
        val Empty = ShiftGeometry(emptyList(), 0, false)
    }
}

/**
 * Чистый расчёт времени смены: первый слот начинается с начала окна, каждый следующий —
 * с конца предыдущего плюс перерыв. Значения смены — 08:00–12:00, 30 минут, перерыв 5 минут.
 */
object ShiftTimeCalculator {

    fun compute(
        startTime: LocalTime,
        endTime: LocalTime,
        breakMinutes: Int,
        durations: List<Int>
    ): ShiftGeometry {
        val windowStart = startTime.toNanosecondOfDay()
        val windowEnd = endTime.toNanosecondOfDay()
        if (windowEnd <= windowStart || breakMinutes < 0) return ShiftGeometry(emptyList(), 0, true)
        val starts = ArrayList<LocalTime>(durations.size)
        var cursor = windowStart
        var slotsEnd = windowStart
        for (duration in durations) {
            val durationNanos = RecurringMath.durationNanos(duration) ?: return ShiftGeometry(emptyList(), 0, true)
            if (cursor >= RecurringMath.NANOS_PER_DAY || cursor + durationNanos > RecurringMath.NANOS_PER_DAY) {
                return ShiftGeometry(emptyList(), 0, true)
            }
            starts += LocalTime.fromNanosecondOfDay(cursor)
            slotsEnd = cursor + durationNanos
            cursor = slotsEnd + breakMinutes * NANOS_PER_MINUTE
        }
        if (durations.isEmpty()) return ShiftGeometry(emptyList(), minutesBetween(windowStart, windowEnd), false)
        val free = if (slotsEnd >= windowEnd) 0 else minutesBetween(slotsEnd, windowEnd)
        return ShiftGeometry(starts, free, slotsEnd > windowEnd)
    }

    private fun minutesBetween(fromNanos: Long, toNanos: Long): Int =
        ((toNanos - fromNanos) / NANOS_PER_MINUTE).toInt()

    private const val NANOS_PER_MINUTE = RecurringMath.NANOS_PER_MINUTE
}