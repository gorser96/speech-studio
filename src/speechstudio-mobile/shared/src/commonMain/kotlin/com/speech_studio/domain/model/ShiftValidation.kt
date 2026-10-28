package com.speech_studio.domain.model

enum class ShiftIssue {
    EmptyDays,
    MissingEndDate,
    InvalidWindow,
    NonPositiveBaseDuration,
    NegativeBreak,
    NonPositiveSlotDuration,
    Unrepresentable
}

/**
 * Чистые проверки шаблона и смены: окно, базовая длительность, перерыв, дни недели и слоты.
 */
object ShiftValidation {

    fun templateIssue(template: ShiftTemplate): ShiftIssue? {
        if (template.daysOfWeek.isEmpty() || template.daysOfWeek.any { it !in 1..7 }) return ShiftIssue.EmptyDays
        return windowIssue(template.startTime, template.endTime, template.baseDurationMinutes, template.breakMinutes)
    }

    fun missingEndDate(template: ShiftTemplate): Boolean = template.endDate == null

    fun shiftIssue(shift: WorkShift): ShiftIssue? =
        windowIssue(shift.startTime, shift.endTime, shift.baseDurationMinutes, shift.breakMinutes)
            ?: shift.slots.firstOrNull { it.durationMinutes <= 0 }?.let { ShiftIssue.NonPositiveSlotDuration }

    private fun windowIssue(
        startTime: kotlinx.datetime.LocalTime,
        endTime: kotlinx.datetime.LocalTime,
        baseDurationMinutes: Int,
        breakMinutes: Int
    ): ShiftIssue? {
        if (baseDurationMinutes <= 0) return ShiftIssue.NonPositiveBaseDuration
        if (breakMinutes < 0) return ShiftIssue.NegativeBreak
        if (endTime <= startTime) return ShiftIssue.InvalidWindow
        if (RecurringMath.durationNanos(baseDurationMinutes) == null) return ShiftIssue.Unrepresentable
        return null
    }
}