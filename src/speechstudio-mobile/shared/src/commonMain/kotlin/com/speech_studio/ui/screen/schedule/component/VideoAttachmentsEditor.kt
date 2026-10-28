package com.speech_studio.ui.screen.schedule.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.speech_studio.platform.video.rememberVideoEnvironment
import com.speech_studio.ui.components.BentoCard
import com.speech_studio.ui.components.SectionLabel

/**
 * Прикреплённые к занятию видео: одна карточка со строками файлов и действиями.
 * Пустое состояние и ошибки остаются на месте, пока пользователь не изменит выбор.
 */
@Composable
fun VideoAttachmentsEditor(
    videoUris: List<String>,
    onChange: (List<String>) -> Unit,
    enabled: Boolean = true,
    sessionId: String = "",
    onAutoAttach: (() -> Unit)? = null,
    scanInProgress: Boolean = false,
    scanMessage: String? = null
) {
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    var videoError by rememberSaveable { mutableStateOf<String?>(null) }
    var launchedSession by remember { mutableStateOf<String?>(null) }
    val environment = rememberVideoEnvironment()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(text = "Видео")
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
        ) {
            if (videoUris.isEmpty()) {
                Text(
                    text = "Видео не прикреплены",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            } else {
                videoUris.forEachIndexed { index, uri ->
                    key(uri) {
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        VideoAttachmentLink(
                            uri = uri,
                            enabled = enabled,
                            onRemove = { pendingRemoval = uri }
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    onClick = {
                        videoError = null
                        launchedSession = sessionId
                        environment.picker.pickVideos { result ->
                            if (enabled && launchedSession == sessionId) {
                                launchedSession = null
                                if (result.pickerUnavailable) {
                                    videoError = "На устройстве не найдено приложение для выбора видео."
                                } else {
                                    onChange((videoUris + result.uris).distinct())
                                    videoError = if (result.failedCount > 0) {
                                        "Не удалось прикрепить файлов: ${result.failedCount}. Остальные видео добавлены."
                                    } else {
                                        null
                                    }
                                }
                            }
                        }
                    }
                ) { Text("Выбрать видео") }
                val attach = onAutoAttach
                if (attach != null && environment.capabilities.deviceScan) {
                    TextButton(
                        enabled = enabled && !scanInProgress,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        onClick = {
                            videoError = null
                            launchedSession = sessionId
                            environment.requestDeviceScanAccess { granted ->
                                if (enabled && launchedSession == sessionId) {
                                    launchedSession = null
                                    if (granted) {
                                        attach()
                                    } else {
                                        videoError = "Нет разрешения на доступ к видео устройства."
                                    }
                                }
                            }
                        }
                    ) { Text(if (scanInProgress) "Поиск видео…" else "Найти видео занятия") }
                }
            }
        }
    }
    videoError?.let { message ->
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    scanMessage?.let { message ->
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    pendingRemoval?.takeIf { enabled }?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Открепить видео?") },
            text = { Text("Видео будет откреплено от занятия после сохранения. Файл останется на устройстве.") },
            confirmButton = {
                TextButton(enabled = enabled, onClick = {
                    if (enabled) onChange(videoUris - uri)
                    pendingRemoval = null
                }) { Text("Открепить") }
            },
            dismissButton = {
                TextButton(enabled = enabled, onClick = { pendingRemoval = null }) { Text("Отмена") }
            }
        )
    }
}
