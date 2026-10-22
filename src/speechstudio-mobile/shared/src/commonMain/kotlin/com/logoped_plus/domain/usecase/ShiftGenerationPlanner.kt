package com.logoped_plus.domain.usecase

import com.logoped_plus.domain.model.ShiftTemplate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/**
 * Чистый план генерации пустых смен: даты диапазона, подходящие по дням недели шаблона,
 * без уже существующих смен. Занятия не планируются.
 */
object ShiftGenerationPlanner {

    fun plan(
        template: ShiftTemplate,
        from: LocalDate,
        existingDates: Set<LocalDate>
    ): List<LocalDate> {
        val end = template.endDate ?: return emptyList()
        if (template.daysOfWeek.isEmpty() || end < from) return emptyList()
        val days = template.daysOfWeek.filter { it in 1..7 }.toSet()
        val dates = ArrayList<LocalDate>()
        var date = from
        while (date <= end) {
            if (date.dayOfWeek.isoDayNumber in days && date !in existingDates) dates += date
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return dates
    }
}