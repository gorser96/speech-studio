package com.speech_studio.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Слот смены: ребёнок, вычисленное начало, длительность и необязательная связь с занятием.
 * Уникальны позиция внутри смены и занятие (одно занятие принадлежит не более чем одному слоту).
 */
@Entity(
    tableName = "shift_slots",
    indices = [
        Index(value = ["shiftId", "position"], unique = true),
        Index(value = ["lessonId"], unique = true),
        Index("shiftId"),
        Index("childId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = WorkShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChildEntity::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ShiftSlotEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val position: Int,
    val startNanoOfDay: Long,
    val durationMinutes: Int,
    val childId: String,
    val lessonId: String? = null
)