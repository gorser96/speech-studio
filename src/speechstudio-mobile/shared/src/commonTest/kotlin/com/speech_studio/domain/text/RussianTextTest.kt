package com.speech_studio.domain.text

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class RussianTextTest {

    @Test
    fun numericDateFormatUsesTwoDigitDayAndMonth() {
        assertEquals("05.10.2026", RussianText.formatNumericDate(LocalDate(2026, 10, 5)))
        assertEquals("31.12.2026", RussianText.formatNumericDate(LocalDate(2026, 12, 31)))
        assertEquals("01.01.0001", RussianText.formatNumericDate(LocalDate(1, 1, 1)))
    }

    @Test
    fun longDateUsesGenitiveRussianMonth() {
        assertEquals("5 октября 2026", RussianText.formatLongDate(LocalDate(2026, 10, 5)))
        assertEquals("1 января 2026", RussianText.formatLongDate(LocalDate(2026, 1, 1)))
        assertEquals("23 февраля 2026", RussianText.formatLongDate(LocalDate(2026, 2, 23)))
    }

    @Test
    fun dayMonthOmitsYearForCompactSummary() {
        assertEquals("5 октября", RussianText.formatDayMonth(LocalDate(2026, 10, 5)))
        assertEquals("1 января", RussianText.formatDayMonth(LocalDate(2026, 1, 1)))
    }

    @Test
    fun monthNamesAndWeekdaysAreDeterministic() {
        assertEquals("Октябрь", RussianText.monthNominative(10))
        assertEquals("октября", RussianText.monthGenitive(10))
        assertEquals("окт.", RussianText.monthShort(10))
        assertEquals("пн", RussianText.weekdayShort(1))
        assertEquals("вс", RussianText.weekdayShort(7))
        assertEquals("понедельник", RussianText.weekdayFull(1))
        assertEquals("воскресенье", RussianText.weekdayFull(7))
    }

    @Test
    fun dayHeaderAndWeekRangeUseShortRussianNames() {
        assertEquals("Пн, 5 окт. 2026", RussianText.formatDayHeader(LocalDate(2026, 10, 5)))
        assertEquals("5–11 окт. 2026", RussianText.formatWeekRange(LocalDate(2026, 10, 5), LocalDate(2026, 10, 11)))
        assertEquals(
            "28 сент. – 4 окт.",
            RussianText.formatWeekRange(LocalDate(2026, 9, 28), LocalDate(2026, 10, 4))
        )
    }

    @Test
    fun timeIsZeroPadded() {
        assertEquals("09:05", RussianText.formatTime(LocalTime(9, 5)))
        assertEquals("23:59", RussianText.formatTime(LocalTime(23, 59)))
        assertEquals("00:00", RussianText.formatTime(LocalTime(0, 0)))
    }

    @Test
    fun countUsesRussianNounForm() {
        assertEquals("0 занятий", RussianText.formatCount(0, "занятие", "занятия", "занятий"))
        assertEquals("1 занятие", RussianText.formatCount(1, "занятие", "занятия", "занятий"))
        assertEquals("2 занятия", RussianText.formatCount(2, "занятие", "занятия", "занятий"))
        assertEquals("4 занятия", RussianText.formatCount(4, "занятие", "занятия", "занятий"))
        assertEquals("5 занятий", RussianText.formatCount(5, "занятие", "занятия", "занятий"))
        assertEquals("11 занятий", RussianText.formatCount(11, "занятие", "занятия", "занятий"))
        assertEquals("14 занятий", RussianText.formatCount(14, "занятие", "занятия", "занятий"))
        assertEquals("21 занятие", RussianText.formatCount(21, "занятие", "занятия", "занятий"))
        assertEquals("22 занятия", RussianText.formatCount(22, "занятие", "занятия", "занятий"))
        assertEquals("1 смена", RussianText.formatCount(1, "смена", "смены", "смен"))
    }

    @Test
    fun nounFormAgreesWithCount() {
        assertEquals("занятие", RussianText.nounForm(1, "занятие", "занятия", "занятий"))
        assertEquals("занятия", RussianText.nounForm(3, "занятие", "занятия", "занятий"))
        assertEquals("занятий", RussianText.nounForm(0, "занятие", "занятия", "занятий"))
        assertEquals("занятий", RussianText.nounForm(12, "занятие", "занятия", "занятий"))
        assertEquals("смена", RussianText.nounForm(21, "смена", "смены", "смен"))
    }
}
