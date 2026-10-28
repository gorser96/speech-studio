package com.speech_studio.ui.screen.schedule

import com.speech_studio.domain.model.WorkShift
import com.speech_studio.ui.screen.schedule.model.LessonUiModel
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

/**
 * Час по доле высоты полосы смены, если он целиком лежит в окне смены.
 * Нажатие у края окна возвращает `null`: занятие за пределами смены не создаётся.
 */
fun ShiftContainerUi.hourAtVerticalFraction(fraction: Float): Int? {
    if (fraction < 0f || fraction > 1f) return null
    if (endMinute <= startMinute) return null
    val minutes = startMinute + fraction * (endMinute - startMinute)
    val hour = (minutes / 60f).toInt()
    return hour.takeIf { it * 60 >= startMinute && (it + 1) * 60 <= endMinute }
}