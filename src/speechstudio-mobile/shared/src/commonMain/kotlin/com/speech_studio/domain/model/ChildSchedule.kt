package com.speech_studio.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class WeeklyLessonSlot(
    val id: String,
    val dayOfWeek: Int,
    val startTime: LocalTime,
    val durationMinutes: Int
)

data class ChildSchedule(
    val slots: List<WeeklyLessonSlot> = emptyList(),
    val endDate: LocalDate? = null
) {
    val isEmpty: Boolean get() = slots.isEmpty()
}
