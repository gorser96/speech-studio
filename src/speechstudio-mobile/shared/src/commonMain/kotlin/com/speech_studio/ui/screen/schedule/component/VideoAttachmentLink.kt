package com.speech_studio.ui.screen.schedule.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.speech_studio.platform.video.VideoAccessError
import com.speech_studio.platform.video.rememberVideoEnvironment
import kotlinx.coroutines.launch

@Composable
fun VideoAttachmentLink(uri: String, enabled: Boolean = true) {
    val environment = rememberVideoEnvironment()
    val scope = rememberCoroutineScope()
    val currentEnabled by rememberUpdatedState(enabled)
    var checking by remember(uri) { mutableStateOf(false) }
    var error by remember(uri) { mutableStateOf<VideoAccessError?>(null) }
    val fileName by produceState("Видео", uri, environment) {
        value = environment.access.displayName(uri)
    }
    TextButton(enabled = enabled && !checking, modifier = Modifier.fillMaxWidth(), onClick = {
        if (currentEnabled && !checking) {
            checking = true
            error = null
            scope.launch {
                try {
                    error = environment.access.open(uri)
                } finally {
                    checking = false
                }
            }
        }
    }) { Text("▶ $fileName") }
    error?.let { reason ->
        Text(videoAccessErrorText(reason), color = MaterialTheme.colorScheme.error)
    }
}

fun videoAccessErrorText(reason: VideoAccessError): String = when (reason) {
    VideoAccessError.Missing -> "Видеофайл не найден. Прикрепите файл заново."
    VideoAccessError.Permission -> "Нет доступа к видео. Прикрепите файл заново."
    VideoAccessError.InvalidLink -> "Некорректная ссылка на видео. Прикрепите файл заново."
    VideoAccessError.Unreadable -> "Не удалось прочитать видео. Повторите попытку."
    VideoAccessError.NoViewer -> "Не найдено приложение для просмотра видео."
}
