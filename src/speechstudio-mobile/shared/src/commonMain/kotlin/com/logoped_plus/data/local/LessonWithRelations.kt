package com.logoped_plus.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.logoped_plus.domain.model.Lesson
import com.logoped_plus.domain.model.LessonParticipant
import com.logoped_plus.domain.model.VideoAttachment
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

data class LessonWithRelations(
    @Embedded val lesson: LessonEntity,
    @Relation(parentColumn = "id", entityColumn = "lessonId") val participants: List<LessonParticipantEntity>,
    @Relation(parentColumn = "id", entityColumn = "lessonId") val videos: List<LessonVideoEntity>
)

fun LessonWithRelations.toDomain() = Lesson(
    id = lesson.id,
    scheduledAt = LocalDateTime(
        LocalDate.fromEpochDays(lesson.scheduledEpochDay),
        LocalTime.fromNanosecondOfDay(lesson.scheduledNanoOfDay)
    ),
    durationMinutes = lesson.durationMinutes,
    comment = lesson.comment,
    participants = participants.sortedBy { it.ordinal }.map { LessonParticipant(it.childId, it.childName) },
    videoAttachments = videos.sortedBy { it.ordinal }.map { VideoAttachment(it.uri) }
)

fun Lesson.toEntity(position: Long = 0) = LessonEntity(
    position,
    id,
    scheduledAt.date.toEpochDays(),
    scheduledAt.time.toNanosecondOfDay(),
    durationMinutes,
    comment
)
