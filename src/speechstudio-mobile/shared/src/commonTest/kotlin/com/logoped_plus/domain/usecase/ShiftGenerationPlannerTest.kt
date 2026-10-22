package com.logoped_plus.domain.usecase

import com.logoped_plus.domain.model.ShiftTemplate
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ShiftGenerationPlannerTest {

    private val template = ShiftTemplate(
        id = "t1",
        daysOfWeek = listOf(1, 3),
        endDate = LocalDate(2026, 9, 10)
    )

    @Test
    fun plansOnlyMatchingWeekdaysUpToEndDate() {
        // 1 сентября 2026 — вторник; понедельники и среды: 2, 7 и 9 сентября.
        val planned = ShiftGenerationPlanner.plan(template, LocalDate(2026, 9, 1), emptySet())

        assertEquals(
            listOf(LocalDate(2026, 9, 2), LocalDate(2026, 9, 7), LocalDate(2026, 9, 9)),
            planned
        )
    }

    @Test
    fun skipsDatesThatAlreadyHaveShifts() {
        val planned = ShiftGenerationPlanner.plan(
            template, LocalDate(2026, 9, 1), setOf(LocalDate(2026, 9, 7))
        )

        assertEquals(listOf(LocalDate(2026, 9, 2), LocalDate(2026, 9, 9)), planned)
    }

    @Test
    fun startsFromGivenDateInclusive() {
        val planned = ShiftGenerationPlanner.plan(template, LocalDate(2026, 9, 8), emptySet())

        assertEquals(listOf(LocalDate(2026, 9, 9)), planned)
    }

    @Test
    fun missingEndDateOrDaysOrReversedRangePlansNothing() {
        assertEquals(
            emptyList(),
            ShiftGenerationPlanner.plan(template.copy(endDate = null), LocalDate(2026, 9, 1), emptySet())
        )
        assertEquals(
            emptyList(),
            ShiftGenerationPlanner.plan(template.copy(daysOfWeek = emptyList()), LocalDate(2026, 9, 1), emptySet())
        )
        assertEquals(
            emptyList(),
            ShiftGenerationPlanner.plan(template, LocalDate(2026, 10, 1), emptySet())
        )
    }
}