package com.speech_studio.ui.screen.schedule.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.speech_studio.platform.video.VideoAccessError
import com.speech_studio.platform.video.rememberVideoEnvironment
import kotlinx.coroutines.launch

/**
 * Строка видео: плитка воспроизведения, имя файла и необязательное действие
 * «открепить» в том же ряду. Нажатие на строку открывает видео.
 */
@Composable
fun VideoAttachmentLink(
    uri: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    val environment = rememberVideoEnvironment()
    val scope = rememberCoroutineScope()
    val currentEnabled by rememberUpdatedState(enabled)
    var checking by remember(uri) { mutableStateOf(false) }
    var error by remember(uri) { mutableStateOf<VideoAccessError?>(null) }
    val fileName by produceState("Видео", uri, environment) {
        value = environment.access.displayName(uri)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled && !checking, onClickLabel = "Открыть видео") {
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
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (onRemove != null) {
            IconButton(
                enabled = enabled,
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Открепить видео",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    error?.let { reason ->
        Text(
            text = videoAccessErrorText(reason),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 42.dp, end = 8.dp, bottom = 6.dp)
        )
    }
}

fun videoAccessErrorText(reason: VideoAccessError): String = when (reason) {
    VideoAccessError.Missing -> "Видеофайл не найден. Прикрепите файл заново."
    VideoAccessError.Permission -> "Нет доступа к видео. Прикрепите файл заново."
    VideoAccessError.InvalidLink -> "Некорректная ссылка на видео. Прикрепите файл заново."
    VideoAccessError.Unreadable -> "Не удалось прочитать видео. Повторите попытку."
    VideoAccessError.NoViewer -> "Не найдено приложение для просмотра видео."
}
