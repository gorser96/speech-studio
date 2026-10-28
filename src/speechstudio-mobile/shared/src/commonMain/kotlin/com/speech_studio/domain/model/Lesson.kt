package com.speech_studio.domain.model

import com.speech_studio.platform.randomUuidString
import kotlinx.datetime.LocalDateTime

data class Lesson(
    val id: String = randomUuidString(),
    val participants: List<LessonParticipant> = emptyList(),
    val scheduledAt: LocalDateTime,
    val durationMinutes: Int,
    val comment: String = "",
    val videoAttachments: List<VideoAttachment> = emptyList(),
    val type: LessonType = LessonType.INDIVIDUAL,
    val shiftId: String? = null
)

data class LessonParticipant(
    val childId: String,
    val removedName: String? = null
)
