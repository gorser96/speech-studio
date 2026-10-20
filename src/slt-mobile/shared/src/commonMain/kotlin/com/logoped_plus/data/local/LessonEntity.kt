package com.logoped_plus.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "lessons", indices = [Index(value = ["id"], unique = true)])
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val position: Long = 0,
    val id: String,
    val scheduledEpochDay: Long,
    val scheduledNanoOfDay: Long,
    val durationMinutes: Int,
    val comment: String
)
