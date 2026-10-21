package com.logoped_plus.domain.model

enum class ScheduleIssue {
    MissingEndDate,
    InvalidDayOfWeek,
    NonPositiveDuration,
    Unrepresentable
}

object ChildScheduleValidation {
    fun validate(schedule: ChildSchedule): ScheduleIssue? {
        if (schedule.slots.isNotEmpty() && schedule.endDate == null) return ScheduleIssue.MissingEndDate
        for (slot in schedule.slots) {
            if (slot.dayOfWeek !in 1..7) return ScheduleIssue.InvalidDayOfWeek
            if (slot.durationMinutes <= 0) return ScheduleIssue.NonPositiveDuration
            if (RecurringMath.durationNanos(slot.durationMinutes) == null) return ScheduleIssue.Unrepresentable
        }
        schedule.endDate?.let {
            if (RecurringMath.toAbsoluteNanos(it.toEpochDays(), RecurringMath.NANOS_PER_DAY - 1) == null)
                return ScheduleIssue.Unrepresentable
        }
        return null
    }
}
