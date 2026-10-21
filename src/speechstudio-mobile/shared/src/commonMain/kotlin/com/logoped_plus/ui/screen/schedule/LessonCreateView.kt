package com.logoped_plus.ui.screen.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logoped_plus.domain.model.Child
import com.logoped_plus.ui.components.NumberField
import com.logoped_plus.ui.screen.schedule.component.*
import kotlinx.datetime.LocalDate

@Composable
fun LessonCreateView(editor: LessonEditorState, children: List<Child>, childrenReady: Boolean, canSave: Boolean, onAction: (ScheduleAction) -> Unit) {
    LessonForm(editor, children, childrenReady, canSave, onAction)
}

@Composable
internal fun LessonForm(
    editor: LessonEditorState,
    children: List<Child>,
    childrenReady: Boolean,
    canSave: Boolean,
    onAction: (ScheduleAction) -> Unit,
    videoScanInProgress: Boolean = false,
    videoScanMessage: String? = null
) {
    val creating = editor.mode == LessonEditorMode.CREATE
    val enabled = !editor.saving
    fun text(field: LessonTextField, value: String) { if (enabled) onAction(ScheduleAction.ChangeText(editor.sessionId, field, value)) }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (creating) "Новое занятие" else "Редактирование занятия", style = MaterialTheme.typography.titleLarge)
        LessonDateTimeFields(
            date = LocalDate.fromEpochDays(editor.epochDay), onDateChange = { onAction(ScheduleAction.ChangeDate(editor.sessionId, it)) },
            time = editor.time, onTimeChange = { text(LessonTextField.TIME, it) }, enabled = enabled
        )
        val durationInvalid = editor.duration.toIntOrNull()?.let { it > 0 } != true
        Row(Modifier.fillMaxWidth()) {
            NumberField(
                value = editor.duration,
                onValueChange = { text(LessonTextField.DURATION, it) },
                label = "Длительность, мин",
                modifier = Modifier.weight(1f),
                enabled = enabled,
                maxDigits = 4,
                isError = durationInvalid,
                supportingText = if (durationInvalid) "Укажите длительность больше 0 минут" else null
            )
            Spacer(Modifier.weight(1f))
        }
        Text("Дети", style = MaterialTheme.typography.titleMedium)
        ChildMultiSelectField(children = children, selectedChildIds = editor.participants.filter { it.removedName == null }.map { it.childId }.toSet(), enabled = enabled && childrenReady,
            onSelectionChange = { onAction(ScheduleAction.ChangeChildren(editor.sessionId, it.toList())) })
        if (!creating) {
            OutlinedTextField(value = editor.comment, onValueChange = { text(LessonTextField.COMMENT, it) }, enabled = enabled,
                label = { Text("Комментарий специалиста") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            VideoAttachmentsEditor(
                videoUris = editor.videoUris,
                onChange = { onAction(ScheduleAction.ChangeVideos(editor.sessionId, it)) },
                enabled = enabled,
                sessionId = editor.sessionId,
                onAutoAttach = { onAction(ScheduleAction.AutoAttachLessonVideos(editor.sessionId)) },
                scanInProgress = videoScanInProgress,
                scanMessage = videoScanMessage
            )
        }
        Button(enabled = canSave, onClick = { onAction(ScheduleAction.ConfirmLesson(editor.sessionId)) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (editor.saving) "Сохранение…" else if (creating) "Создать занятие" else "Сохранить изменения")
        }
    }
}
