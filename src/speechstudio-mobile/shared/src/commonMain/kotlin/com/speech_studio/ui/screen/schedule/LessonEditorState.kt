package com.speech_studio.ui.screen.schedule

import com.speech_studio.domain.model.Lesson
import com.speech_studio.domain.model.LessonParticipant
import com.speech_studio.domain.model.LessonType
import com.speech_studio.domain.model.VideoAttachment
import com.speech_studio.domain.repository.LessonWriteResult
import com.speech_studio.domain.text.RussianText
import com.speech_studio.platform.randomUuidString
import com.speech_studio.ui.components.parseTimeText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

enum class LessonEditorMode { CREATE, DETAILS, EDIT }
enum class LessonTextField { TIME, DURATION, COMMENT }
sealed interface LessonEditorStatus {
    data object Idle : LessonEditorStatus
    data object Saving : LessonEditorStatus
    data class Failure(val reason: LessonWriteResult.Reason) : LessonEditorStatus
}

data class LessonEditorState(
    val sessionId: String = randomUuidString(),
    val lessonId: String = randomUuidString(),
    val mode: LessonEditorMode,
    val original: Lesson? = null,
    val epochDay: Long,
    val time: String,
    val duration: String = "40",
    val participants: List<LessonParticipant> = emptyList(),
    val comment: String = "",
    val videoUris: List<String> = emptyList(),
    val type: LessonType = LessonType.INDIVIDUAL,
    val status: LessonEditorStatus = LessonEditorStatus.Idle
) {
    val saving: Boolean get() = status is LessonEditorStatus.Saving
    val changed: Boolean get() = toLesson() != original

    /**
     * Число участников с учётом сохранённого типа: индивидуальное — ровно один,
     * групповое при создании — два и более, при редактировании — один и более.
     */
    val participantsValid: Boolean
        get() {
            if (participants.isEmpty()) return false
            if (participants.map { it.childId }.distinct().size != participants.size) return false
            return when {
                type == LessonType.GROUP ->
                    participants.size >= if (mode == LessonEditorMode.CREATE) 2 else 1
                else -> participants.size == 1
            }
        }

    fun toLesson(): Lesson? {
        val timeOfDay = parseTimeText(time) ?: return null
        val minutes = duration.toIntOrNull()?.takeIf { it > 0 } ?: return null
        if (!participantsValid) return null
        val dateTime = try {
            // Material-only edits must retain seconds/nanoseconds from the original.
            val date = LocalDate.fromEpochDays(epochDay)
            val old = original?.scheduledAt
            if (old != null && old.date == date && old.hour == timeOfDay.hour && old.minute == timeOfDay.minute) old
            else LocalDateTime(date, timeOfDay)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return Lesson(lessonId, participants.toList(), dateTime, minutes, comment,
            videoUris.map(::VideoAttachment), type, original?.shiftId)
    }

    companion object {
        fun from(lesson: Lesson, mode: LessonEditorMode = LessonEditorMode.DETAILS) = LessonEditorState(
            lessonId = lesson.id, mode = mode, original = lesson,
            epochDay = lesson.scheduledAt.date.toEpochDays(),
            time = RussianText.formatTime(lesson.scheduledAt.time),
            duration = lesson.durationMinutes.toString(), participants = lesson.participants.toList(),
            comment = lesson.comment, videoUris = lesson.videoAttachments.map { it.uri },
            type = lesson.type
        )
    }
}
