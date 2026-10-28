package com.speech_studio.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.speech_studio.domain.model.ShiftSlot
import com.speech_studio.domain.model.ShiftTemplate
import com.speech_studio.domain.model.WorkShift
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class ShiftWithSlots(
    @Embedded val shift: WorkShiftEntity,
    @Relation(parentColumn = "id", entityColumn = "shiftId") val slots: List<ShiftSlotEntity>
)

fun ShiftWithSlots.toDomain() = WorkShift(
    id = shift.id,
    date = LocalDate.fromEpochDays(shift.epochDay),
    startTime = LocalTime.fromNanosecondOfDay(shift.startNanoOfDay),
    endTime = LocalTime.fromNanosecondOfDay(shift.endNanoOfDay),
    baseDurationMinutes = shift.baseDurationMinutes,
    breakMinutes = shift.breakMinutes,
    templateId = shift.templateId,
    slots = slots.sortedBy { it.position }.map {
        ShiftSlot(it.id, it.position, LocalTime.fromNanosecondOfDay(it.startNanoOfDay), it.durationMinutes, it.childId, it.lessonId)
    }
)

fun ShiftTemplateEntity.toDomain() = ShiftTemplate(
    id = id,
    name = name,
    startTime = LocalTime.fromNanosecondOfDay(startNanoOfDay),
    endTime = LocalTime.fromNanosecondOfDay(endNanoOfDay),
    baseDurationMinutes = baseDurationMinutes,
    breakMinutes = breakMinutes,
    daysOfWeek = daysMaskToWeekdays(daysMask),
    endDate = endEpochDay?.let { LocalDate.fromEpochDays(it) }
)

fun ShiftTemplate.toEntity() = ShiftTemplateEntity(
    id = id,
    name = name,
    startNanoOfDay = startTime.toNanosecondOfDay(),
    endNanoOfDay = endTime.toNanosecondOfDay(),
    baseDurationMinutes = baseDurationMinutes,
    breakMinutes = breakMinutes,
    daysMask = weekdaysToDaysMask(daysOfWeek),
    endEpochDay = endDate?.toEpochDays()
)

internal fun weekdaysToDaysMask(daysOfWeek: List<Int>): Int =
    daysOfWeek.filter { it in 1..7 }.fold(0) { mask, day -> mask or (1 shl (day - 1)) }

internal fun daysMaskToWeekdays(mask: Int): List<Int> = (1..7).filter { mask and (1 shl (it - 1)) != 0 }