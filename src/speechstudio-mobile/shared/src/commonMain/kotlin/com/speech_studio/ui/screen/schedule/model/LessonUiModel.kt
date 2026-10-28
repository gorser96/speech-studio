package com.speech_studio.ui.screen.schedule.model

import com.speech_studio.domain.model.LessonType
import com.speech_studio.domain.model.Tag
import kotlinx.datetime.LocalDateTime

data class LessonParticipantUi(
    val name: String,
    val tags: List<Tag> = emptyList()
)

data class LessonUiModel(
    val id: String,
    val scheduledAt: LocalDateTime,
    val durationMinutes: Int,
    val childNames: String,
    val comment: String,
    val participantCount: Int = 0,
    val videoUris: List<String> = emptyList(),
    val participants: List<LessonParticipantUi> = emptyList(),
    val type: LessonType = LessonType.INDIVIDUAL,
    val shiftId: String? = null
)
