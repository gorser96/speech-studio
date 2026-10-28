package com.speech_studio.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "child_schedule", primaryKeys = ["childId"],
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)]
)
data class ChildScheduleEntity(val childId: String, val endEpochDay: Long)
