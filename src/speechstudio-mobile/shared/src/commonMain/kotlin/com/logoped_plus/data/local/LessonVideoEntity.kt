package com.logoped_plus.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "lesson_videos", primaryKeys = ["lessonId", "uri"],
    indices = [Index(value = ["lessonId", "ordinal"], unique = true)],
    foreignKeys = [ForeignKey(entity = LessonEntity::class, parentColumns = ["id"], childColumns = ["lessonId"], onDelete = ForeignKey.CASCADE)]
)
data class LessonVideoEntity(val lessonId: String, val uri: String, val ordinal: Int)
