package com.logoped_plus.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Занятие: тип фиксируется при создании, `shiftId` — необязательная ссылка на смену.
 * Ссылка на смену обнуляется при удалении смены, занятие остаётся.
 */
@Entity(
    tableName = "lessons",
    indices = [Index(value = ["id"], unique = true), Index("shiftId")],
    foreignKeys = [
        ForeignKey(
            entity = WorkShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val position: Long = 0,
    val id: String,
    val scheduledEpochDay: Long,
    val scheduledNanoOfDay: Long,
    val durationMinutes: Int,
    val comment: String,
    val type: String,
    val shiftId: String? = null
)