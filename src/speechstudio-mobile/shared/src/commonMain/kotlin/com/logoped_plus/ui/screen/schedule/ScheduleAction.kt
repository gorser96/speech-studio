package com.logoped_plus.ui.screen.schedule

import com.logoped_plus.domain.model.LessonType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

sealed interface ScheduleAction {
    data object RetryChildren : ScheduleAction
    data object RetryLessons : ScheduleAction
    data object RetryShifts : ScheduleAction
    data class SelectDate(val date: LocalDate) : ScheduleAction
    data class SelectLesson(val lessonId: String) : ScheduleAction
    data object CloseLesson : ScheduleAction
    data object StartEditingLesson : ScheduleAction
    data object CancelEditingLesson : ScheduleAction
    data class StartCreatingLesson(
        val scheduledAt: LocalDateTime? = null,
        val type: LessonType = LessonType.INDIVIDUAL
    ) : ScheduleAction
    data object CancelCreatingLesson : ScheduleAction
    data class StartCreatingShift(val date: LocalDate? = null) : ScheduleAction
    data class OpenShift(val shiftId: String) : ScheduleAction
    data object CloseShift : ScheduleAction
    data class ChangeText(val sessionId: String, val field: LessonTextField, val value: String) : ScheduleAction
    data class ChangeDate(val sessionId: String, val date: LocalDate) : ScheduleAction
    data class ChangeChildren(val sessionId: String, val ids: List<String>) : ScheduleAction
    data class ChangeVideos(val sessionId: String, val uris: List<String>) : ScheduleAction
    data class AutoAttachLessonVideos(val sessionId: String) : ScheduleAction
    data class ConfirmLesson(val sessionId: String) : ScheduleAction
    data class DeleteLesson(val sessionId: String) : ScheduleAction
    data class ChangeViewMode(val mode: ScheduleViewMode) : ScheduleAction
    data object PreviousMonth : ScheduleAction
    data object NextMonth : ScheduleAction
    data object PreviousWeek : ScheduleAction
    data object NextWeek : ScheduleAction
    data object PreviousDay : ScheduleAction
    data object NextDay : ScheduleAction
    data class OpenDay(val date: LocalDate) : ScheduleAction
}
