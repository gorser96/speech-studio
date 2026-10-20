package com.logoped_plus.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "lesson_participants", primaryKeys = ["lessonId", "childId"],
    indices = [Index(value = ["lessonId", "ordinal"], unique = true), Index("childId")],
    foreignKeys = [
        ForeignKey(entity = LessonEntity::class, parentColumns = ["id"], childColumns = ["lessonId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class LessonParticipantEntity(val lessonId: String, val childId: String, val childName: String?, val ordinal: Int)
