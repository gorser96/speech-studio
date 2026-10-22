package com.logoped_plus.ui.screen.schedule

import com.logoped_plus.domain.model.WorkShift
import com.logoped_plus.ui.screen.schedule.model.LessonUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Данные контейнера смены для сетки и списка расписания: окно и число созданных
 * занятий. Чистые данные без Compose, пригодные для теста.
 */
data class ShiftContainerUi(
    val shiftId: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val lessonCount: Int
) {
    val startMinute: Int get() = startTime.hour * 60 + startTime.minute
    val endMinute: Int get() = endTime.hour * 60 + endTime.minute
}

/** Строит контейнеры смен, считая занятия по `lessons.shiftId`. */
fun buildShiftContainers(
    shifts: List<WorkShift>,
    lessons: List<LessonUiModel>
): List<ShiftContainerUi> = shifts.map { shift ->
    ShiftContainerUi(
        shiftId = shift.id,
        date = shift.date,
        startTime = shift.startTime,
        endTime = shift.endTime,
        lessonCount = lessons.count { it.shiftId == shift.id }
    )
}

/** Контейнеры смен на конкретную дату, отсортированные по началу окна. */
fun List<ShiftContainerUi>.onDate(date: LocalDate): List<ShiftContainerUi> =
    filter { it.date == date }.sortedBy { it.startMinute }