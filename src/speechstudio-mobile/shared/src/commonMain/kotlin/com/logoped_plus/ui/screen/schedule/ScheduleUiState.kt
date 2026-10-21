package com.logoped_plus.ui.screen.schedule

import com.logoped_plus.domain.model.Child
import com.logoped_plus.domain.repository.ChildLoadState
import com.logoped_plus.domain.repository.LessonLoadState
import com.logoped_plus.ui.screen.schedule.model.LessonUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class ScheduleUiState(
    val lessonLoadState: LessonLoadState = LessonLoadState.Loading(),
    val editor: LessonEditorState? = null,
    val detailsDraft: LessonEditorState? = null,
    val awaitingSnapshot: Boolean = false,
    val childLoadState: ChildLoadState = ChildLoadState.Loading(),
    val selectedLesson: LessonUiModel? = null,
    val isCreatingLesson: Boolean = false,
    val creationDateTime: LocalDateTime? = null,
    val isEditingLesson: Boolean = false,
    val viewMode: ScheduleViewMode = ScheduleViewMode.WEEK,
    val selectedDate: LocalDate,
    val displayedMonth: LocalDate,
    val displayedWeekStart: LocalDate,
    val lessons: List<LessonUiModel> = emptyList(),
    val children: List<Child> = emptyList(),
    val weekendHidden: Boolean = false,
    val gridStartHour: Int = 0,
    val gridEndHour: Int = 24,
    val visibleTagsLimit: Int = 3,
    val videoScanInProgress: Boolean = false,
    val videoScanMessage: String? = null
) {
    val saving: Boolean get() = editor?.saving == true
    val canSave: Boolean
        get() = !saving && !awaitingSnapshot &&
                childLoadState is ChildLoadState.Ready && lessonLoadState is LessonLoadState.Ready &&
                editor?.toLesson()?.let { lesson ->
                    lesson.participants.all { p -> p.removedName != null || children.any { it.id == p.childId } } &&
                            (editor.mode == LessonEditorMode.CREATE || lessonLoadState.lessons.any { it.id == lesson.id })
                } == true
    val canDelete: Boolean
        get() = !saving && !awaitingSnapshot &&
                childLoadState is ChildLoadState.Ready && lessonLoadState is LessonLoadState.Ready &&
                editor?.let { e -> e.mode != LessonEditorMode.CREATE && lessonLoadState.lessons.any { it.id == e.lessonId } } == true
}
