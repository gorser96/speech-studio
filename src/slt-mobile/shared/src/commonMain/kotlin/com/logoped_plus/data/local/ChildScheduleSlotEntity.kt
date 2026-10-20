package com.logoped_plus.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "child_schedule_slots", primaryKeys = ["childId", "id"],
    indices = [Index(value = ["childId", "ordinal"], unique = true)],
    foreignKeys = [
        ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ChildScheduleEntity::class, parentColumns = ["childId"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class ChildScheduleSlotEntity(
    val childId: String,
    val id: String,
    val dayOfWeek: Int,
    val startNanoOfDay: Long,
    val durationMinutes: Int,
    val ordinal: Int
)
