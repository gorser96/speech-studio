package com.speech_studio.ui.screen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.speech_studio.platform.PlatformBackHandler
import com.speech_studio.ui.screen.schedule.LessonCreateView
import com.speech_studio.ui.screen.schedule.LessonDetailsView
import com.speech_studio.ui.screen.schedule.LessonEditView
import com.speech_studio.ui.screen.schedule.MonthScheduleView
import com.speech_studio.ui.screen.schedule.ScheduleAction
import com.speech_studio.ui.screen.schedule.ScheduleUiState
import com.speech_studio.ui.screen.schedule.ScheduleViewMode
import com.speech_studio.ui.screen.schedule.ScheduleViewModel
import com.speech_studio.ui.screen.schedule.WeekScheduleView
import com.speech_studio.ui.screen.shift.ShiftScreen
import com.speech_studio.ui.screen.shift.ShiftViewModel
import com.speech_studio.domain.repository.*
import com.speech_studio.ui.screen.schedule.*

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    shiftViewModel: ShiftViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    val openShiftId = uiState.openShiftId
    if (openShiftId != null) {
        val shiftState by shiftViewModel.uiState.collectAsState()
        PlatformBackHandler(enabled = true) {
            if (!shiftState.working) viewModel.onAction(ScheduleAction.CloseShift)
        }
        ShiftScreen(
            shiftId = openShiftId,
            viewModel = shiftViewModel,
            onClose = { viewModel.onAction(ScheduleAction.CloseShift) }
        )
        return
    }

    ScheduleContent(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
internal fun ScheduleContent(
    uiState: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ChildrenLoadStatus(uiState.childLoadState, { onAction(ScheduleAction.RetryChildren) })
        LessonLoadStatus(uiState, onAction)
        ShiftLoadStatus(uiState, onAction)
        ScheduleBody(uiState, onAction)
    }
}

@Composable
private fun ScheduleBody(
    uiState: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit
) {
    val scrollState = rememberScrollState()
    val selectedLesson = uiState.selectedLesson
    val shiftContainers = buildShiftContainers(uiState.shifts, uiState.lessons)
    val childrenReady = uiState.childLoadState is ChildLoadState.Ready
    val editor = uiState.editor
    PlatformBackHandler(enabled = editor != null) {
        if (!uiState.saving) onAction(when (editor?.mode) {
            LessonEditorMode.CREATE -> ScheduleAction.CancelCreatingLesson
            LessonEditorMode.EDIT -> ScheduleAction.CancelEditingLesson
            else -> ScheduleAction.CloseLesson
        })
    }
    if (editor != null) {
        androidx.compose.runtime.key(editor.sessionId) {
            when (editor.mode) {
                LessonEditorMode.CREATE -> LessonCreateView(editor, uiState.children, childrenReady, uiState.canSave, onAction)
                LessonEditorMode.EDIT -> LessonEditView(editor, uiState.children, childrenReady, uiState.canSave, onAction,
                    uiState.videoScanInProgress, uiState.videoScanMessage)
                LessonEditorMode.DETAILS -> selectedLesson?.let {
                    LessonDetailsView(canSave = uiState.canSave, canDelete = uiState.canDelete, editor = editor, uiModel = it,
                        visibleTagsLimit = uiState.visibleTagsLimit,
                        onEdit = { onAction(ScheduleAction.StartEditingLesson) },
                        onDelete = { onAction(ScheduleAction.DeleteLesson(editor.sessionId)) },
                        comment = editor.comment, videoUris = editor.videoUris,
                        onCommentChange = { value -> onAction(ScheduleAction.ChangeText(editor.sessionId, LessonTextField.COMMENT, value)) },
                        onVideosChange = { uris -> onAction(ScheduleAction.ChangeVideos(editor.sessionId, uris)) },
                        onAutoAttach = { onAction(ScheduleAction.AutoAttachLessonVideos(editor.sessionId)) },
                        videoScanInProgress = uiState.videoScanInProgress,
                        videoScanMessage = uiState.videoScanMessage,
                        onSave = { onAction(ScheduleAction.ConfirmLesson(editor.sessionId)) })
                }
            }
        }
        return
    }
    if (uiState.lessonLoadState !is LessonLoadState.Ready && uiState.lessons.isEmpty()) {
        return
    }
    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    val currentMode by rememberUpdatedState(uiState.viewMode)
    val currentOnAction by rememberUpdatedState(onAction)
    BoxWithConstraints(
        Modifier.fillMaxSize().pointerInput(swipeThreshold) {
            var dragDistance = 0f
            detectHorizontalDragGestures(
                onDragStart = { dragDistance = 0f },
                onDragCancel = { dragDistance = 0f },
                onDragEnd = {
                    val nextMode = when {
                        dragDistance <= -swipeThreshold -> when (currentMode) {
                            ScheduleViewMode.MONTH -> ScheduleViewMode.WEEK
                            ScheduleViewMode.WEEK -> ScheduleViewMode.DAY
                            ScheduleViewMode.DAY -> null
                        }
                        dragDistance >= swipeThreshold -> when (currentMode) {
                            ScheduleViewMode.DAY -> ScheduleViewMode.WEEK
                            ScheduleViewMode.WEEK -> ScheduleViewMode.MONTH
                            ScheduleViewMode.MONTH -> null
                        }
                        else -> null
                    }
                    dragDistance = 0f
                    nextMode?.let { currentOnAction(ScheduleAction.ChangeViewMode(it)) }
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    dragDistance += dragAmount
                }
            )
        }
    ) {
        val weekHeight = maxHeight.coerceAtLeast(420.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            ScheduleViewModeSelector(
                selectedMode = uiState.viewMode,
                onModeSelected = { mode ->
                    onAction(
                        ScheduleAction.ChangeViewMode(mode)
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (uiState.viewMode) {
                ScheduleViewMode.MONTH -> {
                    MonthScheduleView(
                        displayedMonth = uiState.displayedMonth,
                        selectedDate = uiState.selectedDate,
                        lessons = uiState.lessons,
                        onDateSelected = { date ->
                            onAction(
                                ScheduleAction.SelectDate(date)
                            )
                        },
                        onPreviousMonth = {
                            onAction(ScheduleAction.PreviousMonth)
                        },
                        onNextMonth = {
                            onAction(ScheduleAction.NextMonth)
                        },
                        onLessonClick = { lesson ->
                            onAction(
                                ScheduleAction.SelectLesson(lesson.id)
                            )
                        },
                        shifts = shiftContainers,
                        onShiftClick = { shiftId ->
                            onAction(ScheduleAction.OpenShift(shiftId))
                        }
                    )
                }

                ScheduleViewMode.WEEK -> {
                    WeekScheduleView(
                        onEmptyHourDoubleClick = { dateTime ->
                            onAction(ScheduleAction.StartCreatingLesson(dateTime))
                        },
                        modifier = Modifier.height(weekHeight),
                        weekStart = uiState.displayedWeekStart,
                        lessons = uiState.lessons,
                        weekendHidden = uiState.weekendHidden,
                        gridStartHour = uiState.gridStartHour,
                        gridEndHour = uiState.gridEndHour,
                        onPreviousWeek = {
                            onAction(ScheduleAction.PreviousWeek)
                        },
                        onNextWeek = {
                            onAction(ScheduleAction.NextWeek)
                        },
                        onLessonClick = { uiModel ->
                            onAction(
                                ScheduleAction.SelectLesson(uiModel.id)
                            )
                        },
                        onOpenDay = { date ->
                            onAction(ScheduleAction.OpenDay(date))
                        },
                        shifts = shiftContainers,
                        onShiftClick = { shiftId ->
                            onAction(ScheduleAction.OpenShift(shiftId))
                        }
                    )
                }

                ScheduleViewMode.DAY -> {
                    DayScheduleView(
                        onEmptyHourDoubleClick = { dateTime ->
                            onAction(ScheduleAction.StartCreatingLesson(dateTime))
                        },
                        modifier = Modifier.height(weekHeight),
                        date = uiState.selectedDate,
                        lessons = uiState.lessons,
                        gridStartHour = uiState.gridStartHour,
                        gridEndHour = uiState.gridEndHour,
                        onPreviousDay = {
                            onAction(ScheduleAction.PreviousDay)
                        },
                        onNextDay = {
                            onAction(ScheduleAction.NextDay)
                        },
                        onLessonClick = { uiModel ->
                            onAction(
                                ScheduleAction.SelectLesson(uiModel.id)
                            )
                        },
                        shifts = shiftContainers,
                        onShiftClick = { shiftId ->
                            onAction(ScheduleAction.OpenShift(shiftId))
                        }
                    )
                }
            }

        }
    }
}

@Composable
private fun ScheduleViewModeSelector(
    selectedMode: ScheduleViewMode, onModeSelected: (ScheduleViewMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        SegmentedButton(
            selected = selectedMode == ScheduleViewMode.MONTH, onClick = {
                onModeSelected(ScheduleViewMode.MONTH)
            }, shape = SegmentedButtonDefaults.itemShape(
                index = 0, count = 3, baseShape = RoundedCornerShape(8.dp)
            )
        ) {
            Text("Месяц")
        }

        SegmentedButton(
            selected = selectedMode == ScheduleViewMode.WEEK, onClick = {
                onModeSelected(ScheduleViewMode.WEEK)
            }, shape = SegmentedButtonDefaults.itemShape(
                index = 1, count = 3, baseShape = RoundedCornerShape(8.dp)
            )
        ) {
            Text("Неделя")
        }

        SegmentedButton(
            selected = selectedMode == ScheduleViewMode.DAY, onClick = {
                onModeSelected(ScheduleViewMode.DAY)
            }, shape = SegmentedButtonDefaults.itemShape(
                index = 2, count = 3, baseShape = RoundedCornerShape(8.dp)
            )
        ) {
            Text("День")
        }
    }
}

@Composable
private fun ShiftLoadStatus(state: ScheduleUiState, onAction: (ScheduleAction) -> Unit) {
    when (state.shiftLoadState) {
        is ShiftLoadState.Loading -> Text("Загрузка смен...")
        is ShiftLoadState.Error -> {
            Text("Не удалось загрузить смены")
            Button(enabled = !state.saving, onClick = { onAction(ScheduleAction.RetryShifts) }) { Text("Повторить загрузку смен") }
        }
        is ShiftLoadState.Ready -> Unit
    }
    state.shiftMessage?.let { message -> Text(message, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun LessonLoadStatus(state: ScheduleUiState, onAction: (ScheduleAction) -> Unit) {
    when (state.lessonLoadState) {
        is LessonLoadState.Loading -> Text(if (state.awaitingSnapshot) "Изменения сохранены. Обновление расписания…" else "Загрузка занятий…")
        is LessonLoadState.Error -> {
            Text(if (state.awaitingSnapshot) "Изменения сохранены. Не удалось обновить расписание" else "Не удалось загрузить занятия")
            Button(enabled = !state.saving, onClick = { onAction(ScheduleAction.RetryLessons) }) { Text("Повторить загрузку занятий") }
        }
        is LessonLoadState.Ready -> if (state.awaitingSnapshot) Text("Изменения сохранены. Обновление расписания…")
    }
    val reason = (state.editor?.status as? LessonEditorStatus.Failure)?.reason
    if (reason != null) {
        Text(when (reason) {
            LessonWriteResult.Reason.StorageUnavailable -> "Не удалось сохранить занятие. Повторите сохранение."
            LessonWriteResult.Reason.NotReady -> "Дождитесь загрузки данных и повторите сохранение."
            LessonWriteResult.Reason.InvalidData -> "Проверьте дату, время, длительность и участников."
            LessonWriteResult.Reason.UnknownChild -> "Участник не найден. Обновите данные детей и исправьте выбор."
            LessonWriteResult.Reason.NotFound -> "Занятие не найдено. Обновите данные. Черновик сохранён."
            LessonWriteResult.Reason.Conflict -> "Занятие с этим идентификатором уже существует. Обновите данные."
        })
        if (reason in listOf(LessonWriteResult.Reason.NotFound, LessonWriteResult.Reason.Conflict, LessonWriteResult.Reason.NotReady)) {
            Button(enabled = !state.saving && state.lessonLoadState !is LessonLoadState.Loading,
                onClick = { onAction(ScheduleAction.RetryLessons) }) { Text("Обновить данные") }
        }
    }
}
