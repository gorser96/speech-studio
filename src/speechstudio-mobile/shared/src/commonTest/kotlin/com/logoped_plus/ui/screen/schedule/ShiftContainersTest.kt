package com.logoped_plus.ui.screen.schedule

import com.logoped_plus.domain.model.LessonType
import com.logoped_plus.domain.model.WorkShift
import com.logoped_plus.ui.screen.schedule.model.LessonUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ShiftContainersTest {

    private val date = LocalDate(2026, 5, 4)

    @Test
    fun containerKeepsWindowAndCountsOnlyShiftLessons() {
        val shift = WorkShift("s1", date, LocalTime(8, 0), LocalTime(11, 0))
        val lessons = listOf(
            lesson("l1", "s1", 8),
            lesson("l2", "s1", 9),
            lesson("l3", "other", 10),
            lesson("l4", null, 10)
        )

        val container = buildShiftContainers(listOf(shift), lessons).single()

        assertEquals("s1", container.shiftId)
        assertEquals(date, container.date)
        assertEquals(8 * 60, container.startMinute)
        assertEquals(11 * 60, container.endMinute)
        assertEquals(2, container.lessonCount)
    }

    @Test
    fun onDateFiltersOtherDaysAndSortsByStart() {
        val early = WorkShift("early", date, LocalTime(8, 0), LocalTime(11, 0))
        val late = WorkShift("late", date, LocalTime(13, 0), LocalTime(16, 0))
        val otherDay = WorkShift("other", date.plusDays(1))

        val containers = buildShiftContainers(listOf(late, otherDay, early), emptyList())

        assertEquals(listOf("early", "late"), containers.onDate(date).map { it.shiftId })
    }

    private fun lesson(id: String, shiftId: String?, hour: Int) = LessonUiModel(
        id = id,
        scheduledAt = LocalDateTime(date, LocalTime(hour, 0)),
        durationMinutes = 30,
        childNames = "Ребёнок",
        comment = "",
        type = LessonType.INDIVIDUAL,
        shiftId = shiftId
    )
}