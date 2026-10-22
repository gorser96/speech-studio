@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ui.screen.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logoped_plus.data.preferences.SettingsRepository
import com.logoped_plus.domain.model.Lesson
import com.logoped_plus.domain.model.LessonParticipant
import com.logoped_plus.domain.model.LessonType
import com.logoped_plus.domain.model.WorkShift
import com.logoped_plus.domain.repository.*
import com.logoped_plus.domain.text.RussianText
import com.logoped_plus.domain.usecase.FindLessonVideosUseCase
import com.logoped_plus.platform.randomUuidString
import com.logoped_plus.ui.screen.schedule.model.LessonParticipantUi
import com.logoped_plus.ui.screen.schedule.model.LessonUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ScheduleViewModel(
    private val lessonRepository: LessonRepository,
    private val childRepository: ChildRepository,
    private val tagRepository: TagRepository,
    private val shiftRepository: ShiftRepository,
    private val settings: SettingsRepository,
    private val findLessonVideos: FindLessonVideosUseCase,
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ViewModel() {
    private val today = clock.now().toLocalDateTime(timeZone).date
    private var confirmed: Lesson? = null
    private val mutableState = MutableStateFlow(ScheduleUiState(
        selectedDate = today, displayedMonth = today.firstOfMonth(), displayedWeekStart = today.startOfWeek(),
        childLoadState = childRepository.state.value, lessonLoadState = lessonRepository.state.value,
        shiftLoadState = shiftRepository.state.value, shifts = shiftRepository.state.value.shifts,
        children = childRepository.state.value.children, weekendHidden = settings.hideWeekend.value,
        gridStartHour = settings.gridStartHour.value, gridEndHour = settings.gridEndHour.value,
        visibleTagsLimit = settings.visibleTagsLimit.value
    ))
    val uiState = mutableState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            combine(childRepository.state, lessonRepository.state, tagRepository.state, shiftRepository.state) { _, _, _, _ -> Unit }
                .collect { refresh() }
        }
        viewModelScope.launch {
            settings.hideWeekend.collect { hidden -> mutableState.value = mutableState.value.copy(weekendHidden = hidden) }
        }
        viewModelScope.launch {
            settings.gridStartHour.collect { hour -> mutableState.value = mutableState.value.copy(gridStartHour = hour) }
        }
        viewModelScope.launch {
            settings.gridEndHour.collect { hour -> mutableState.value = mutableState.value.copy(gridEndHour = hour) }
        }
        viewModelScope.launch {
            settings.visibleTagsLimit.collect { limit -> mutableState.value = mutableState.value.copy(visibleTagsLimit = limit) }
        }
    }

    private fun refresh(
        children: ChildLoadState = childRepository.state.value,
        loaded: LessonLoadState = lessonRepository.state.value,
        tags: TagLoadState = tagRepository.state.value,
        shifts: ShiftLoadState = shiftRepository.state.value
    ) {
        val committed = confirmed
        if (loaded is LessonLoadState.Ready && committed != null && loaded.lessons.any { it == committed }) confirmed = null
        val overlay = confirmed
        val rows = if (overlay == null) loaded.lessons else if (loaded.lessons.any { it.id == overlay.id })
            loaded.lessons.map { if (it.id == overlay.id) overlay else it } else loaded.lessons + overlay
        val state = mutableState.value
        val models = rows.map { toUiModel(it, children, tags) }
        // A deleted lesson leaves no row, so an open details/edit on it has nothing to show.
        val closed = state.editor != null && state.editor?.mode != LessonEditorMode.CREATE &&
            rows.none { it.id == state.editor?.lessonId }
        val editor = if (closed) null else state.editor
        // A missing row must not discard a draft. Names can change independently of its raw input.
        val selected = editor?.takeIf { it.mode != LessonEditorMode.CREATE }?.original?.let { toUiModel(it, children, tags) }
        mutableState.value = state.copy(childLoadState = children, lessonLoadState = loaded, children = children.children,
            shiftLoadState = shifts, shifts = shifts.shifts,
            lessons = models, editor = editor, detailsDraft = if (closed) null else state.detailsDraft,
            selectedLesson = selected, isCreatingLesson = editor?.mode == LessonEditorMode.CREATE,
            isEditingLesson = editor?.mode == LessonEditorMode.EDIT,
            creationDateTime = if (editor?.mode == LessonEditorMode.CREATE) LocalDate.fromEpochDays(editor.epochDay).atStartOfDay() else null,
            awaitingSnapshot = confirmed != null)
    }

    fun onAction(action: ScheduleAction) {
        if (mutableState.value.saving) return
        val state = mutableState.value
        when (action) {
            ScheduleAction.RetryChildren -> childRepository.retryLoading()
            ScheduleAction.RetryLessons -> lessonRepository.retryLoading()
            ScheduleAction.RetryShifts -> shiftRepository.retryLoading()
            is ScheduleAction.StartCreatingLesson -> {
                val date = action.scheduledAt ?: state.selectedDate.atStartOfDay()
                setEditor(LessonEditorState(mode = LessonEditorMode.CREATE, epochDay = date.date.toEpochDays(),
                    time = RussianText.formatTime(date.time), type = action.type))
            }
            is ScheduleAction.StartCreatingShift -> startCreatingShift(action.date ?: state.selectedDate)
            is ScheduleAction.OpenShift -> mutableState.value = state.copy(openShiftId = action.shiftId, shiftMessage = null)
            ScheduleAction.CloseShift -> mutableState.value = state.copy(openShiftId = null, shiftMessage = null)
            is ScheduleAction.SelectLesson -> {
                val lesson = confirmed?.takeIf { it.id == action.lessonId }
                    ?: lessonRepository.state.value.lessons.find { it.id == action.lessonId } ?: return
                setEditor(LessonEditorState.from(lesson))
            }
            ScheduleAction.StartEditingLesson -> {
                val editor = state.editor?.takeIf { it.mode == LessonEditorMode.DETAILS } ?: return
                mutableState.value = state.copy(detailsDraft = editor)
                setEditor(editor.copy(sessionId = randomUuidString(), mode = LessonEditorMode.EDIT, status = LessonEditorStatus.Idle), keepDetails = true)
            }
            ScheduleAction.CancelEditingLesson -> setEditor(state.detailsDraft)
            ScheduleAction.CloseLesson, ScheduleAction.CancelCreatingLesson -> setEditor(null)
            is ScheduleAction.ChangeText -> change(action.sessionId) { editor ->
                if (editor.mode == LessonEditorMode.DETAILS && action.field != LessonTextField.COMMENT) editor
                else when (action.field) {
                    LessonTextField.TIME -> editor.copy(time = action.value)
                    LessonTextField.DURATION -> editor.copy(duration = action.value)
                    LessonTextField.COMMENT -> if (editor.mode == LessonEditorMode.CREATE) editor else editor.copy(comment = action.value)
                }
            }
            is ScheduleAction.ChangeDate -> change(action.sessionId) { if (it.mode == LessonEditorMode.DETAILS) it else it.copy(epochDay = action.date.toEpochDays()) }
            is ScheduleAction.ChangeChildren -> change(action.sessionId) { if (it.mode == LessonEditorMode.DETAILS) it
                else if (it.type == LessonType.GROUP)
                    it.copy(participants = action.ids.distinct().map { id -> LessonParticipant(id) } + it.participants.filter { it.removedName != null })
                else it.copy(participants = action.ids.distinct().map { id -> LessonParticipant(id) }) }
            is ScheduleAction.ChangeVideos -> change(action.sessionId) { if (it.mode == LessonEditorMode.CREATE) it else it.copy(videoUris = action.uris.distinct()) }
            is ScheduleAction.AutoAttachLessonVideos -> autoAttach(action.sessionId)
            is ScheduleAction.ConfirmLesson -> confirm(action.sessionId)
            is ScheduleAction.DeleteLesson -> delete(action.sessionId)
            is ScheduleAction.SelectDate -> mutableState.value = state.copy(selectedDate = action.date)
            is ScheduleAction.ChangeViewMode -> mutableState.value = state.copy(viewMode = action.mode,
                displayedMonth = state.selectedDate.firstOfMonth(), displayedWeekStart = state.selectedDate.startOfWeek())
            ScheduleAction.PreviousMonth -> mutableState.value = state.copy(displayedMonth = state.displayedMonth.minusMonths(1))
            ScheduleAction.NextMonth -> mutableState.value = state.copy(displayedMonth = state.displayedMonth.plusMonths(1))
            ScheduleAction.PreviousWeek -> mutableState.value = state.copy(displayedWeekStart = state.displayedWeekStart.minusWeeks(1))
            ScheduleAction.NextWeek -> mutableState.value = state.copy(displayedWeekStart = state.displayedWeekStart.plusWeeks(1))
            ScheduleAction.PreviousDay -> mutableState.value = state.copy(selectedDate = state.selectedDate.minusDays(1))
            ScheduleAction.NextDay -> mutableState.value = state.copy(selectedDate = state.selectedDate.plusDays(1))
            is ScheduleAction.OpenDay -> mutableState.value = state.copy(selectedDate = action.date, viewMode = ScheduleViewMode.DAY,
                displayedMonth = action.date.firstOfMonth(), displayedWeekStart = action.date.startOfWeek())
        }
    }

    private fun change(sessionId: String, transform: (LessonEditorState) -> LessonEditorState) {
        val editor = mutableState.value.editor?.takeIf { it.sessionId == sessionId } ?: return
        setEditor(transform(editor).copy(status = LessonEditorStatus.Idle), keepDetails = true)
    }

    private fun startCreatingShift(date: LocalDate) {
        if (shiftRepository.state.value !is ShiftLoadState.Ready) {
            mutableState.value = mutableState.value.copy(shiftMessage = "Дождитесь загрузки смен и повторите.")
            return
        }
        val shift = WorkShift(id = randomUuidString(), date = date)
        mutableState.value = mutableState.value.copy(shiftMessage = null)
        viewModelScope.launch {
            when (val result = shiftRepository.createShift(shift)) {
                ShiftWriteResult.Success -> mutableState.value = mutableState.value.copy(openShiftId = shift.id)
                is ShiftWriteResult.Failure ->
                    mutableState.value = mutableState.value.copy(shiftMessage = shiftWriteErrorText(result.reason))
            }
        }
    }

    private fun setEditor(editor: LessonEditorState?, keepDetails: Boolean = false) {
        mutableState.value = mutableState.value.copy(editor = editor,
            detailsDraft = if (keepDetails) mutableState.value.detailsDraft else null,
            isCreatingLesson = editor?.mode == LessonEditorMode.CREATE,
            isEditingLesson = editor?.mode == LessonEditorMode.EDIT,
            creationDateTime = if (editor?.mode == LessonEditorMode.CREATE) LocalDate.fromEpochDays(editor.epochDay).atStartOfDay() else null,
            videoScanInProgress = false, videoScanMessage = null)
        refresh()
    }

    private fun autoAttach(sessionId: String) {
        val state = mutableState.value
        if (state.videoScanInProgress) return
        val editor = state.editor?.takeIf { it.sessionId == sessionId } ?: return
        val lesson = editor.toLesson()
        if (lesson == null) {
            mutableState.value = state.copy(videoScanMessage = "Исправьте дату, время или длительность занятия.")
            return
        }
        mutableState.value = state.copy(videoScanInProgress = true, videoScanMessage = null)
        viewModelScope.launch {
            val found = findLessonVideos(lesson.scheduledAt, lesson.durationMinutes, editor.videoUris.toSet())
            val current = mutableState.value
            val currentEditor = current.editor?.takeIf { it.sessionId == sessionId }
            if (currentEditor == null || current.saving) {
                mutableState.value = current.copy(videoScanInProgress = false)
                return@launch
            }
            val added = found.filter { it.uri !in currentEditor.videoUris }
            if (added.isEmpty()) {
                mutableState.value = current.copy(videoScanInProgress = false, videoScanMessage = "Видео за время занятия не найдены.")
            } else {
                onAction(ScheduleAction.ChangeVideos(sessionId, (currentEditor.videoUris + added.map { it.uri }).distinct()))
                mutableState.value = mutableState.value.copy(videoScanInProgress = false, videoScanMessage = "Добавлено видео: ${added.size}.")
            }
        }
    }

    private fun confirm(sessionId: String) {
        // Read repository readiness now, even when its collector has not been dispatched yet.
        refresh()
        val state = mutableState.value
        val editor = state.editor?.takeIf { it.sessionId == sessionId } ?: return
        if (!state.canSave) return
        if (editor.mode == LessonEditorMode.DETAILS && !editor.changed) return
        val lesson = editor.toLesson() ?: return
        setEditor(editor.copy(status = LessonEditorStatus.Saving), keepDetails = true)
        viewModelScope.launch {
            val result = if (editor.mode == LessonEditorMode.CREATE) lessonRepository.addLesson(lesson) else lessonRepository.updateLesson(lesson)
            when (result) {
                is LessonWriteResult.Failure -> setEditor(editor.copy(status = LessonEditorStatus.Failure(result.reason)), keepDetails = true)
                is LessonWriteResult.Success -> {
                    confirmed = result.lesson
                    setEditor(if (editor.mode == LessonEditorMode.CREATE) null else LessonEditorState.from(result.lesson))
                }
            }
        }
    }

    private fun delete(sessionId: String) {
        refresh()
        val state = mutableState.value
        val editor = state.editor?.takeIf { it.sessionId == sessionId } ?: return
        if (!state.canDelete) return
        setEditor(editor.copy(status = LessonEditorStatus.Saving), keepDetails = true)
        confirmed = null
        viewModelScope.launch {
            when (val result = lessonRepository.deleteLesson(editor.lessonId)) {
                is LessonWriteResult.Failure -> setEditor(editor.copy(status = LessonEditorStatus.Failure(result.reason)), keepDetails = true)
                is LessonWriteResult.Success -> setEditor(null)
            }
        }
    }

    private fun toUiModel(lesson: Lesson, children: ChildLoadState, tags: TagLoadState): LessonUiModel {
        val byId = children.children.associateBy { it.id }
        val tagsInOrder = (tags as? TagLoadState.Ready)?.tags.orEmpty()
        val names = lesson.participants.map { it.removedName ?: byId[it.childId]?.name }.filterNotNull().joinToString(", ")
        val participants = lesson.participants.map { p ->
            val child = byId[p.childId]
            val assigned = child?.tagIds.orEmpty().toSet()
            // Directory order is the display order.
            LessonParticipantUi(p.removedName ?: child?.name ?: "", tagsInOrder.filter { it.id in assigned })
        }
        return LessonUiModel(lesson.id, lesson.scheduledAt, lesson.durationMinutes,
            names, lesson.comment, lesson.participants.size,
            lesson.videoAttachments.map { it.uri }, participants, lesson.type, lesson.shiftId)
    }
}
