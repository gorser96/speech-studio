package com.logoped_plus.domain.usecase

import com.logoped_plus.domain.model.ChildSchedule
import com.logoped_plus.domain.model.GenerationCounts
import com.logoped_plus.domain.model.LessonSpan
import com.logoped_plus.domain.model.RecurringMath
import com.logoped_plus.domain.model.ScheduleIssue
import com.logoped_plus.domain.model.ChildScheduleValidation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber

data class PlannedLesson(
    val childId: String,
    val startEpochDay: Long,
    val startNanoOfDay: Long,
    val durationMinutes: Int
)

sealed interface RecurringPlanResult {
    data class Valid(val lessons: List<PlannedLesson>, val counts: GenerationCounts) : RecurringPlanResult
    data class Invalid(val issue: ScheduleIssue) : RecurringPlanResult
}

object RecurringLessonPlanner {
    fun plan(
        schedule: ChildSchedule,
        childId: String,
        now: LocalDateTime,
        childLessonSpans: List<LessonSpan>
    ): RecurringPlanResult {
        ChildScheduleValidation.validate(schedule)?.let { return RecurringPlanResult.Invalid(it) }
        val slots = schedule.slots
        val end = schedule.endDate
        if (slots.isEmpty() || end == null) return RecurringPlanResult.Valid(emptyList(), GenerationCounts())
        val today = now.date
        if (end < today) return RecurringPlanResult.Valid(emptyList(), GenerationCounts())
        val nowAbs = RecurringMath.toAbsoluteNanos(today.toEpochDays(), now.time.toNanosecondOfDay())
            ?: return RecurringPlanResult.Invalid(ScheduleIssue.Unrepresentable)
        val existingStarts = HashSet<Long>(childLessonSpans.size)
        for (span in childLessonSpans) {
            val start = RecurringMath.toAbsoluteNanos(span.epochDay, span.nanoOfDay)
                ?: return RecurringPlanResult.Invalid(ScheduleIssue.Unrepresentable)
            existingStarts.add(start)
        }
        val addedStarts = HashSet<Long>()
        val lessons = ArrayList<PlannedLesson>()
        var existing = 0
        for (epochDay in today.toEpochDays()..end.toEpochDays()) {
            val dayOfWeek = LocalDate.fromEpochDays(epochDay).dayOfWeek.isoDayNumber
            for (slot in slots.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime.toNanosecondOfDay() }) {
                val startNano = slot.startTime.toNanosecondOfDay()
                val start = RecurringMath.toAbsoluteNanos(epochDay, startNano)
                    ?: return RecurringPlanResult.Invalid(ScheduleIssue.Unrepresentable)
                if (start < nowAbs) continue
                if (start in existingStarts || start in addedStarts) {
                    existing++
                    continue
                }
                RecurringMath.intervalEnd(start, slot.durationMinutes)
                    ?: return RecurringPlanResult.Invalid(ScheduleIssue.Unrepresentable)
                lessons.add(PlannedLesson(childId, epochDay, startNano, slot.durationMinutes))
                addedStarts.add(start)
            }
        }
        return RecurringPlanResult.Valid(lessons, GenerationCounts(lessons.size, existing))
    }
}
