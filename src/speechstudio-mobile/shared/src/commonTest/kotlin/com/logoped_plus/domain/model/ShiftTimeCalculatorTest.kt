package com.logoped_plus.domain.model

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShiftTimeCalculatorTest {

    private fun starts(vararg hoursMinutes: Pair<Int, Int>) = hoursMinutes.map { LocalTime(it.first, it.second) }

    @Test
    fun slotsFollowOrderWithBreak() {
        val geometry = ShiftTimeCalculator.compute(
            startTime = LocalTime(8, 0), endTime = LocalTime(12, 0), breakMinutes = 5,
            durations = listOf(30, 30, 40)
        )

        assertEquals(starts(8 to 0, 8 to 35, 9 to 10), geometry.starts)
        assertEquals(130, geometry.freeMinutes)
        assertFalse(geometry.overflows)
    }

    @Test
    fun breakChangeMovesFollowingSlots() {
        val geometry = ShiftTimeCalculator.compute(
            startTime = LocalTime(8, 0), endTime = LocalTime(12, 0), breakMinutes = 10,
            durations = listOf(30, 30)
        )

        assertEquals(starts(8 to 0, 8 to 40), geometry.starts)
        assertEquals(170, geometry.freeMinutes)
    }

    @Test
    fun durationChangeRecomputesFollowingSlots() {
        val geometry = ShiftTimeCalculator.compute(
            startTime = LocalTime(8, 0), endTime = LocalTime(12, 0), breakMinutes = 5,
            durations = listOf(40, 30, 30)
        )

        assertEquals(starts(8 to 0, 8 to 45, 9 to 20), geometry.starts)
        assertEquals(130, geometry.freeMinutes)
    }

    @Test
    fun removingSlotRecomputesFreeTime() {
        val geometry = ShiftTimeCalculator.compute(
            startTime = LocalTime(8, 0), endTime = LocalTime(12, 0), breakMinutes = 5,
            durations = listOf(30, 30)
        )

        assertEquals(starts(8 to 0, 8 to 35), geometry.starts)
        assertEquals(175, geometry.freeMinutes)
        assertFalse(geometry.overflows)
    }

    @Test
    fun overflowingSlotsAreReportedWithoutFreeTime() {
        val geometry = ShiftTimeCalculator.compute(
            startTime = LocalTime(8, 0), endTime = LocalTime(9, 0), breakMinutes = 5,
            durations = listOf(30, 30, 30)
        )

        assertEquals(starts(8 to 0, 8 to 35, 9 to 10), geometry.starts)
        assertEquals(0, geometry.freeMinutes)
        assertTrue(geometry.overflows)
    }

    @Test
    fun emptyShiftShowsWholeWindowAsFree() {
        val geometry = ShiftTimeCalculator.compute(
            startTime = LocalTime(8, 0), endTime = LocalTime(12, 0), breakMinutes = 5,
            durations = emptyList()
        )

        assertTrue(geometry.starts.isEmpty())
        assertEquals(240, geometry.freeMinutes)
        assertFalse(geometry.overflows)
    }

    @Test
    fun invalidWindowOrBreakIsRejected() {
        val reversed = ShiftTimeCalculator.compute(LocalTime(12, 0), LocalTime(8, 0), 5, listOf(30))
        val same = ShiftTimeCalculator.compute(LocalTime(8, 0), LocalTime(8, 0), 5, listOf(30))
        val negativeBreak = ShiftTimeCalculator.compute(LocalTime(8, 0), LocalTime(12, 0), -1, listOf(30))

        listOf(reversed, same, negativeBreak).forEach {
            assertTrue(it.starts.isEmpty())
            assertTrue(it.overflows)
        }
    }
}