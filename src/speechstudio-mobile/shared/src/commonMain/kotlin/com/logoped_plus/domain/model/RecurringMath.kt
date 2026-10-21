package com.logoped_plus.domain.model

object RecurringMath {
    const val NANOS_PER_MINUTE = 60_000_000_000L
    const val NANOS_PER_DAY = 86_400_000_000_000L

    fun toAbsoluteNanos(epochDay: Long, nanoOfDay: Long): Long? {
        val days = multiplyExact(epochDay, NANOS_PER_DAY) ?: return null
        return addExact(days, nanoOfDay)
    }

    fun durationNanos(durationMinutes: Int): Long? =
        multiplyExact(durationMinutes.toLong(), NANOS_PER_MINUTE)

    fun intervalEnd(startAbsoluteNanos: Long, durationMinutes: Int): Long? {
        val duration = durationNanos(durationMinutes) ?: return null
        return addExact(startAbsoluteNanos, duration)
    }

    private fun addExact(left: Long, right: Long): Long? {
        val result = left + right
        return if ((left xor result) and (right xor result) < 0) null else result
    }

    private fun multiplyExact(left: Long, right: Long): Long? {
        if (left == 0L || right == 0L) return 0L
        if (left == Long.MIN_VALUE && right == -1L) return null
        if (right == Long.MIN_VALUE && left == -1L) return null
        val result = left * right
        return if (result / right != left) null else result
    }
}
