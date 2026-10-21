package com.logoped_plus.ui.screen.schedule.component

import androidx.compose.material3.AlertDialog
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
import com.logoped_plus.platform.video.rememberVideoEnvironment

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

    Text("Видео", style = MaterialTheme.typography.titleMedium)
    videoUris.forEach { uri ->
        key(uri) {
            VideoAttachmentLink(uri, enabled = enabled)
            TextButton(enabled = enabled, onClick = { pendingRemoval = uri }) {
                Text("Открепить")
            }
        }
    }
    TextButton(
        enabled = enabled,
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
            onClick = {
                videoError = null
                launchedSession = sessionId
                environment.requestDeviceScanAccess { granted ->
                    if (enabled && launchedSession == sessionId) {
                        launchedSession = null
                        if (granted) attach() else videoError = "Нет разрешения на доступ к видео устройства."
                    }
                }
            }
        ) { Text(if (scanInProgress) "Поиск видео…" else "Найти видео занятия") }
    }
    videoError?.let { message ->
        Text(message, color = MaterialTheme.colorScheme.error)
    }
    scanMessage?.let { message ->
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
