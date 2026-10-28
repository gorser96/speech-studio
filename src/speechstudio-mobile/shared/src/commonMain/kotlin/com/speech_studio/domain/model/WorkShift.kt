package com.speech_studio.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Значения новой смены без шаблона. */
val DEFAULT_SHIFT_START: LocalTime = LocalTime(8, 0)
val DEFAULT_SHIFT_END: LocalTime = LocalTime(12, 0)
const val DEFAULT_SHIFT_BASE_DURATION_MINUTES = 30
const val DEFAULT_SHIFT_BREAK_MINUTES = 5

/**
 * Шаблон рабочей смены: только сетка времени и дни повторения, без состава детей.
 */
data class ShiftTemplate(
    val id: String,
    val name: String = "",
    val startTime: LocalTime = DEFAULT_SHIFT_START,
    val endTime: LocalTime = DEFAULT_SHIFT_END,
    val baseDurationMinutes: Int = DEFAULT_SHIFT_BASE_DURATION_MINUTES,
    val breakMinutes: Int = DEFAULT_SHIFT_BREAK_MINUTES,
    /** Дни недели по ISO: 1 — понедельник … 7 — воскресенье. */
    val daysOfWeek: List<Int> = emptyList(),
    val endDate: LocalDate? = null
)

/**
 * Слот смены: ребёнок, вычисленное начало, длительность и связь с созданным занятием.
 */
data class ShiftSlot(
    val id: String,
    val position: Int,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val childId: String,
    val lessonId: String? = null
)

/**
 * Смена на дату со снимком окна, базовой длительности и перерыва.
 */
data class WorkShift(
    val id: String,
    val date: LocalDate,
    val startTime: LocalTime = DEFAULT_SHIFT_START,
    val endTime: LocalTime = DEFAULT_SHIFT_END,
    val baseDurationMinutes: Int = DEFAULT_SHIFT_BASE_DURATION_MINUTES,
    val breakMinutes: Int = DEFAULT_SHIFT_BREAK_MINUTES,
    val templateId: String? = null,
    val slots: List<ShiftSlot> = emptyList()
) {
    /** Число занятий, созданных по слотам смены. */
    val lessonCount: Int get() = slots.count { it.lessonId != null }
}