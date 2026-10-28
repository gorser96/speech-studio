package com.speech_studio.ui.screen.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import com.speech_studio.domain.text.RussianText
import com.speech_studio.ui.screen.LimitedTagChips
import com.speech_studio.ui.screen.schedule.model.LessonUiModel
import com.speech_studio.ui.screen.schedule.component.VideoAttachmentsEditor

@Composable
fun LessonDetailsView(
    canSave: Boolean,
    canDelete: Boolean,
    editor: LessonEditorState,
    uiModel: LessonUiModel,
    visibleTagsLimit: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    comment: String,
    videoUris: List<String>,
    onCommentChange: (String) -> Unit,
    onVideosChange: (List<String>) -> Unit,
    onAutoAttach: () -> Unit,
    videoScanInProgress: Boolean,
    videoScanMessage: String?,
    onSave: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Занятие",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(enabled = canDelete && !editor.saving, onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить занятие")
            }
            IconButton(enabled = !editor.saving, onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать занятие")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LessonDetailsRow(
            title = "Дата",
            value = RussianText.formatLongDatePadded(uiModel.scheduledAt.date)
        )

        HorizontalDivider()

        LessonDetailsRow(
            title = "Время",
            value = uiModel.scheduledAt.time.toString()
        )

        HorizontalDivider()

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ребёнок",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            uiModel.participants.forEach { participant ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = participant.name,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (participant.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        LimitedTagChips(participant.tags, visibleTagsLimit.takeIf { it in 0..10 } ?: 3)
                    }
                }
            }
            if (uiModel.participants.isEmpty()) Text(uiModel.childNames)
        }

        HorizontalDivider()

        LessonDetailsRow(
            title = "Тип",
            value = lessonTypeTitle(uiModel.type)
        )

        HorizontalDivider()
        LessonDetailsRow("Длительность", "${uiModel.durationMinutes} мин")
        HorizontalDivider()
        OutlinedTextField(
            enabled = !editor.saving, value = comment,
            onValueChange = onCommentChange,
            label = { Text("Комментарий специалиста") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        )
        VideoAttachmentsEditor(
            videoUris = videoUris,
            onChange = onVideosChange,
            enabled = !editor.saving,
            sessionId = editor.sessionId,
            onAutoAttach = onAutoAttach,
            scanInProgress = videoScanInProgress,
            scanMessage = videoScanMessage
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (comment != uiModel.comment || videoUris != uiModel.videoUris) {
            Button(
                enabled = canSave,
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить занятие?") },
            text = { Text("Запись, участники, комментарий и привязки видео будут удалены. Файлы видео на устройстве не удаляются.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun LessonDetailsRow(
    title: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
