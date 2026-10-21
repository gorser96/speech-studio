package com.logoped_plus.ui.screen.schedule

import com.logoped_plus.ui.screen.schedule.model.LessonUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeekTimelineTest {

    private val date = LocalDate(2026, 9, 21)
    private val nextDay = date.plusDays(1)

    @Test
    fun singleLessonTakesFullWidthWithoutLanes() {
        val timeline = buildDayTimeline(date, listOf(lesson("a", 9, 0, 60)))

        val span = timeline.spans.single()
        assertEquals("a", span.lesson.id)
        assertNull(span.lane)
        assertNull(span.laneCount)
        assertEquals(540f, span.startMinute, 0.001f)
        assertEquals(600f, span.endMinute, 0.001f)
        assertTrue(timeline.isHourEmpty(8))
        assertFalse(timeline.isHourEmpty(9))
        assertTrue(timeline.isHourEmpty(10))
    }

    @Test
    fun twoOverlappingLessonsBecomeTwoLanesInStartOrder() {
        val timeline = buildDayTimeline(date, listOf(
            lesson("b", 9, 30, 30),
            lesson("a", 9, 0, 120)
        ))

        assertEquals(listOf("a", "b"), timeline.spans.map { it.lesson.id })
        assertEquals(0, timeline.spans[0].lane)
        assertEquals(1, timeline.spans[1].lane)
        assertEquals(2, timeline.spans[0].laneCount)
        assertEquals(2, timeline.spans[1].laneCount)
        assertFalse(timeline.isHourEmpty(9))
        assertFalse(timeline.isHourEmpty(10))
    }

    @Test
    fun threeMutuallyOverlappingLessonsGetThreeLanes() {
        val timeline = buildDayTimeline(date, listOf(
            lesson("c", 10, 0, 90),
            lesson("b", 9, 30, 60),
            lesson("a", 9, 0, 120)
        ))

        assertEquals(listOf("a", "b", "c"), timeline.spans.map { it.lesson.id })
        assertEquals(listOf(0, 1, 2), timeline.spans.map { it.lane })
        assertEquals(3, timeline.spans[0].laneCount)
        assertEquals(3, timeline.spans[1].laneCount)
        assertEquals(3, timeline.spans[2].laneCount)
        assertFalse(timeline.isHourEmpty(11))
    }

    @Test
    fun touchingIntervalsAreNotOverlapping() {
        val timeline = buildDayTimeline(date, listOf(
            lesson("a", 9, 0, 30),
            lesson("b", 9, 30, 30)
        ))

        assertEquals(listOf("a", "b"), timeline.spans.map { it.lesson.id })
        timeline.spans.forEach { span ->
            assertNull(span.lane)
            assertNull(span.laneCount)
        }
    }

    @Test
    fun chainedOverlapsFormOneClusterWithTwoLanes() {
        val timeline = buildDayTimeline(date, listOf(
            lesson("a", 9, 0, 60),
            lesson("b", 9, 30, 60),
            lesson("c", 10, 15, 45)
        ))

        assertEquals(listOf("a", "b", "c"), timeline.spans.map { it.lesson.id })
        assertEquals(listOf(0, 1, 0), timeline.spans.map { it.lane })
        assertEquals(2, timeline.spans[0].laneCount)
        assertEquals(2, timeline.spans[1].laneCount)
        assertEquals(2, timeline.spans[2].laneCount)
    }

    @Test
    fun midnightSplitKeepsDayLayingOutIndependent() {
        val night = LessonUiModel(
            id = "night",
            scheduledAt = date.atTime(23, 30),
            durationMinutes = 60,
            childNames = "night",
            comment = ""
        )

        val firstDay = buildDayTimeline(date, listOf(
            night,
            lesson("late", 23, 45, 30)
        ))
        assertEquals(listOf("night", "late"), firstDay.spans.map { it.lesson.id })
        assertEquals(0, firstDay.spans[0].lane)
        assertEquals(1, firstDay.spans[1].lane)
        assertEquals(2, firstDay.spans[0].laneCount)
        assertEquals(2, firstDay.spans[1].laneCount)

        val secondDay = buildDayTimeline(nextDay, listOf(
            night,
            lesson("early", 0, 15, 45, nextDay)
        ))
        assertEquals(listOf("night", "early"), secondDay.spans.map { it.lesson.id })
        assertEquals(0f, secondDay.spans[0].startMinute, 0.001f)
        assertEquals(30f, secondDay.spans[0].endMinute, 0.001f)
        assertEquals(0, secondDay.spans[0].lane)
        assertEquals(1, secondDay.spans[1].lane)
        assertEquals(2, secondDay.spans[0].laneCount)
        assertEquals(2, secondDay.spans[1].laneCount)
    }

    @Test
    fun gridRangeKeepsOnlySpansFullyInside() {
        val timeline = buildDayTimeline(date, listOf(
            lesson("inside", 9, 0, 60),
            lesson("crossing-start", 7, 30, 60),
            lesson("crossing-end", 15, 30, 60),
            lesson("outside", 17, 0, 30)
        ), startMinute = 480, endMinute = 960)

        assertEquals(listOf("inside"), timeline.spans.map { it.lesson.id })
    }

    @Test
    fun gridRangeKeepsSpansTouchingBoundaries() {
        val timeline = buildDayTimeline(date, listOf(
            lesson("at-start", 8, 0, 30),
            lesson("at-end", 15, 30, 30)
        ), startMinute = 480, endMinute = 960)

        assertEquals(listOf("at-start", "at-end"), timeline.spans.map { it.lesson.id })
        assertEquals(480f, timeline.spans[0].startMinute, 0.001f)
        assertEquals(960f, timeline.spans[1].endMinute, 0.001f)
    }

    private fun lesson(
        id: String,
        hour: Int,
        minute: Int,
        durationMinutes: Int,
        day: LocalDate = date
    ): LessonUiModel = LessonUiModel(
        id = id,
        scheduledAt = day.atTime(LocalTime(hour, minute)),
        durationMinutes = durationMinutes,
        childNames = id,
        comment = ""
    )
}
