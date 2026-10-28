package com.speech_studio.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.speech_studio.domain.model.Child
import com.speech_studio.domain.model.ChildSchedule
import com.speech_studio.domain.model.WeeklyLessonSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class ChildCardWithRelations(
    @Embedded val child: ChildEntity,
    @Relation(parentColumn = "id", entityColumn = "childId") val tags: List<ChildTagEntity>,
    @Relation(parentColumn = "id", entityColumn = "childId") val schedule: ChildScheduleEntity?,
    @Relation(parentColumn = "id", entityColumn = "childId") val slots: List<ChildScheduleSlotEntity>
)

fun ChildCardWithRelations.toDomain() = Child(
    id = child.id,
    name = child.name,
    tagIds = tags.map { it.tagId },
    schedule = ChildSchedule(
        slots = slots.sortedBy { it.ordinal }.map {
            WeeklyLessonSlot(it.id, it.dayOfWeek, LocalTime.fromNanosecondOfDay(it.startNanoOfDay), it.durationMinutes)
        },
        endDate = schedule?.let { LocalDate.fromEpochDays(it.endEpochDay) }
    )
)
