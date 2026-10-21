package com.logoped_plus.domain.usecase

import com.logoped_plus.domain.model.ChildSchedule
import com.logoped_plus.domain.model.GenerationCounts
import com.logoped_plus.domain.model.LessonSpan
import com.logoped_plus.domain.model.ScheduleIssue
import com.logoped_plus.domain.model.WeeklyLessonSlot
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurringLessonPlannerTest {
    private val child = "child-1"

    private fun slot(day: Int, time: String, minutes: Int, id: String = "s${time}d$day") =
        WeeklyLessonSlot(id, day, LocalTime.parse(time), minutes)

    private fun span(date: LocalDate, time: String, minutes: Int) =
        LessonSpan(date.toEpochDays(), LocalTime.parse(time).toNanosecondOfDay(), minutes)

    private fun plan(
        schedule: ChildSchedule,
        now: LocalDateTime,
        childSpans: List<LessonSpan> = emptyList()
    ) = RecurringLessonPlanner.plan(schedule, child, now, childSpans)

    private fun valid(result: RecurringPlanResult): Pair<List<PlannedLesson>, GenerationCounts> {
        assertTrue(result is RecurringPlanResult.Valid, "expected valid plan, got $result")
        val plan = result as RecurringPlanResult.Valid
        return plan.lessons to plan.counts
    }

    private fun invalid(result: RecurringPlanResult): ScheduleIssue {
        assertTrue(result is RecurringPlanResult.Invalid, "expected invalid plan, got $result")
        return (result as RecurringPlanResult.Invalid).issue
    }

    @Test
    fun october2026CreatesEightLessons() {
        val schedule = ChildSchedule(
            slots = listOf(slot(4, "16:00", 40), slot(5, "15:00", 30)),
            endDate = LocalDate(2026, 10, 31)
        )
        val (lessons, counts) = valid(plan(schedule, LocalDateTime(2026, 10, 5, 9, 0)))
        assertEquals(GenerationCounts(8, 0), counts)
        val expected = listOf(
            Triple(LocalDate(2026, 10, 8), "16:00", 40),
            Triple(LocalDate(2026, 10, 9), "15:00", 30),
            Triple(LocalDate(2026, 10, 15), "16:00", 40),
            Triple(LocalDate(2026, 10, 16), "15:00", 30),
            Triple(LocalDate(2026, 10, 22), "16:00", 40),
            Triple(LocalDate(2026, 10, 23), "15:00", 30),
            Triple(LocalDate(2026, 10, 29), "16:00", 40),
            Triple(LocalDate(2026, 10, 30), "15:00", 30)
        )
        assertEquals(expected.size, lessons.size)
        lessons.forEachIndexed { index, planned ->
            val (date, time, minutes) = expected[index]
            assertEquals(date.toEpochDays(), planned.startEpochDay)
            assertEquals(LocalTime.parse(time).toNanosecondOfDay(), planned.startNanoOfDay)
            assertEquals(minutes, planned.durationMinutes)
            assertEquals(child, planned.childId)
        }
    }

    @Test
    fun startedLessonIsSkippedOnLastDay() {
        val lastDay = LocalDate(2026, 10, 8)
        val schedule = ChildSchedule(listOf(slot(4, "15:00", 30), slot(4, "16:00", 30)), lastDay)
        val (lessons, counts) = valid(plan(schedule, lastDay.atTime(15, 0, 30)))
        assertEquals(GenerationCounts(1, 0), counts)
        assertEquals(LocalTime(16, 0).toNanosecondOfDay(), lessons.single().startNanoOfDay)
        val (both, countsAtExactStart) = valid(plan(schedule, lastDay.atTime(15, 0, 0)))
        assertEquals(GenerationCounts(2, 0), countsAtExactStart)
        assertEquals(2, both.size)
    }

    @Test
    fun repeatSkipsExistingIncludingGroupLessons() {
        val schedule = ChildSchedule(
            slots = listOf(slot(4, "16:00", 40), slot(5, "15:00", 30)),
            endDate = LocalDate(2026, 10, 16)
        )
        val now = LocalDateTime(2026, 10, 5, 9, 0)
        val group = span(LocalDate(2026, 10, 9), "15:00", 90)
        val (first, firstCounts) = valid(plan(schedule, now, childSpans = listOf(group)))
        assertEquals(GenerationCounts(3, 1), firstCounts)
        val created = first.map { LessonSpan(it.startEpochDay, it.startNanoOfDay, it.durationMinutes) }
        val (second, secondCounts) = valid(plan(schedule, now, childSpans = created + group))
        assertEquals(GenerationCounts(0, 4), secondCounts)
        assertTrue(second.isEmpty())
    }

    @Test
    fun overlapsAcrossAllChildrenAreCreated() {
        val day = LocalDate(2026, 10, 8)
        val schedule = ChildSchedule(listOf(slot(4, "16:20", 30), slot(4, "16:40", 30)), day)
        val (lessons, counts) = valid(plan(schedule, day.atTime(9, 0), childSpans = listOf(span(day, "16:00", 40))))
        assertEquals(GenerationCounts(2, 0), counts)
        assertEquals(
            listOf(LocalTime(16, 20), LocalTime(16, 40)).map { it.toNanosecondOfDay() },
            lessons.map { it.startNanoOfDay }
        )
    }

    @Test
    fun touchingBoundariesAreAllowed() {
        val day = LocalDate(2026, 10, 8)
        val schedule = ChildSchedule(listOf(slot(4, "16:00", 20)), day)
        val (lessons, counts) = valid(plan(schedule, day.atTime(9, 0), childSpans = listOf(span(day, "16:20", 20))))
        assertEquals(GenerationCounts(1, 0), counts)
        assertEquals(1, lessons.size)
    }

    @Test
    fun midnightCrossingIsCreated() {
        val today = LocalDate(2026, 10, 8)
        val schedule = ChildSchedule(listOf(slot(4, "00:10", 30)), today)
        val (lessons, counts) = valid(
            plan(
                schedule,
                today.atTime(0, 0),
                childSpans = listOf(span(today.minus(1, DateTimeUnit.DAY), "23:50", 40))
            )
        )
        assertEquals(GenerationCounts(1, 0), counts)
        assertEquals(LocalTime(0, 10).toNanosecondOfDay(), lessons.single().startNanoOfDay)
    }

    @Test
    fun overlappingTemplateRowsAreBothCreated() {
        val day = LocalDate(2026, 10, 8)
        val schedule = ChildSchedule(listOf(slot(4, "16:00", 40), slot(4, "16:20", 30)), day)
        val (lessons, counts) = valid(plan(schedule, day.atTime(9, 0)))
        assertEquals(GenerationCounts(2, 0), counts)
        assertEquals(
            listOf(LocalTime(16, 0), LocalTime(16, 20)).map { it.toNanosecondOfDay() },
            lessons.map { it.startNanoOfDay }
        )
        assertEquals(listOf(40, 30), lessons.map { it.durationMinutes })
    }

    @Test
    fun identicalStartsSecondBecomesExisting() {
        val day = LocalDate(2026, 10, 8)
        val schedule = ChildSchedule(listOf(slot(4, "16:00", 40, "a"), slot(4, "16:00", 30, "b")), day)
        val (lessons, counts) = valid(plan(schedule, day.atTime(9, 0)))
        assertEquals(GenerationCounts(1, 1), counts)
        assertEquals(40, lessons.single().durationMinutes)
    }

    @Test
    fun movedLessonFreesItsOldSlot() {
        val day = LocalDate(2026, 10, 8)
        val schedule = ChildSchedule(listOf(slot(4, "16:00", 40)), day)
        val moved = span(day, "17:00", 40)
        val (lessons, counts) = valid(plan(schedule, day.atTime(9, 0), childSpans = listOf(moved)))
        assertEquals(GenerationCounts(1, 0), counts)
        assertEquals(1, lessons.size)
    }

    @Test
    fun invalidSchedulesAreRejected() {
        val now = LocalDateTime(2026, 10, 5, 9, 0)
        assertEquals(ScheduleIssue.MissingEndDate, invalid(plan(ChildSchedule(listOf(slot(4, "16:00", 40))), now)))
        assertEquals(
            ScheduleIssue.InvalidDayOfWeek,
            invalid(plan(ChildSchedule(listOf(slot(8, "16:00", 40)), LocalDate(2026, 10, 31)), now))
        )
        assertEquals(
            ScheduleIssue.NonPositiveDuration,
            invalid(plan(ChildSchedule(listOf(slot(4, "16:00", 0)), LocalDate(2026, 10, 31)), now))
        )
        assertEquals(
            ScheduleIssue.Unrepresentable,
            invalid(plan(ChildSchedule(listOf(slot(4, "16:00", Int.MAX_VALUE)), LocalDate(2026, 10, 31)), now))
        )
    }

    @Test
    fun emptyScheduleAndPastEndDateCreateNothing() {
        val now = LocalDateTime(2026, 10, 5, 9, 0)
        assertEquals(GenerationCounts(), valid(plan(ChildSchedule(), now)).second)
        val past = ChildSchedule(listOf(slot(4, "16:00", 40)), LocalDate(2026, 10, 4))
        val (lessons, counts) = valid(plan(past, now))
        assertEquals(GenerationCounts(), counts)
        assertTrue(lessons.isEmpty())
    }
}
