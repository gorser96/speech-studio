package com.speech_studio.ui.screen.schedule

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class HourGridTest {

    @Test
    fun initialHourIsEightWhenItFitsTheRange() {
        assertEquals(8, initialGridScrollHour(8, 21))
        assertEquals(8, initialGridScrollHour(0, 24))
        assertEquals(8, initialGridScrollHour(7, 10))
    }

    @Test
    fun initialHourFallsBackToRangeStart() {
        assertEquals(14, initialGridScrollHour(14, 20))
        assertEquals(9, initialGridScrollHour(9, 18))
        // Конечный час исключающий: 08:00 в диапазон 00–08 не входит.
        assertEquals(0, initialGridScrollHour(0, 8))
    }

    @Test
    fun labelsCoverEveryBoundaryOnce() {
        val labels = hourGridLabels(8, 21)

        assertEquals(14, labels.size)
        assertEquals("08:00", labels.first())
        assertEquals("21:00", labels.last())
        assertEquals(1, labels.indexOf("09:00"))
        assertEquals(labels.size, labels.distinct().size)
    }

    @Test
    fun visibleWeekEndFollowsWeekendSetting() {
        val monday = LocalDate(2026, 10, 26)

        assertEquals(LocalDate(2026, 10, 30), visibleWeekEnd(monday, weekendHidden = true))
        assertEquals(LocalDate(2026, 11, 1), visibleWeekEnd(monday, weekendHidden = false))
    }
}
