@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ui.screen.shift.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.logoped_plus.domain.text.RussianText
import com.logoped_plus.ui.screen.schedule.component.VideoAttachmentLink
import com.logoped_plus.ui.screen.shift.ShiftVideoReviewState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Разбор найденных видео смены: превью, время, предполагаемое занятие и исключение.
 * Привязки применяются только после подтверждения.
 */
@Composable
fun ShiftVideoReviewDialog(
    review: ShiftVideoReviewState,
    onSetLesson: (uri: String, lessonId: String?) -> Unit,
    onToggleExcluded: (uri: String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val zone = remember { TimeZone.currentSystemDefault() }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(),
        title = { Text("Найденные видео") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (review.rows.isEmpty()) {
                    Text("Видео за время смены не найдено.")
                }
                review.rows.forEach { row ->
                    var menuOpen by remember(row.uri) { mutableStateOf(false) }
                    val selectedLabel = review.lessons.firstOrNull { it.lessonId == row.effectiveLessonId }?.label
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VideoAttachmentLink(row.uri)
                        }
                        Text(
                            RussianText.formatTime(
                                Instant.fromEpochMilliseconds(row.createdMillis).toLocalDateTime(zone).time
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (row.alreadyAttached) {
                            Text("Уже прикреплено к этому занятию", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (row.ambiguous) {
                            Text("Время попадает в несколько занятий", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { menuOpen = true }) {
                                Text(selectedLabel ?: "Не прикреплять")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Не прикреплять") },
                                    onClick = { menuOpen = false; onSetLesson(row.uri, null) }
                                )
                                review.lessons.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = { menuOpen = false; onSetLesson(row.uri, option.lessonId) }
                                    )
                                }
                            }
                            Checkbox(
                                checked = row.excluded,
                                onCheckedChange = { onToggleExcluded(row.uri) }
                            )
                            Text("Исключить")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Прикрепить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}