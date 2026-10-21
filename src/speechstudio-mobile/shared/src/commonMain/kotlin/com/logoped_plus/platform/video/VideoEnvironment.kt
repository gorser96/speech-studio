package com.logoped_plus.platform.video

import androidx.compose.runtime.Composable

data class VideoPickResult(
    val uris: List<String>,
    val failedCount: Int = 0,
    val pickerUnavailable: Boolean = false
)

enum class VideoAccessError {
    Missing,
    Permission,
    InvalidLink,
    Unreadable,
    NoViewer
}

interface VideoPicker {
    fun pickVideos(onResult: (VideoPickResult) -> Unit)
}

interface VideoAccess {
    suspend fun displayName(uri: String): String
    suspend fun open(uri: String): VideoAccessError?
}

data class VideoCapabilities(
    val deviceScan: Boolean = true
)

class VideoEnvironment(
    val picker: VideoPicker,
    val access: VideoAccess,
    val capabilities: VideoCapabilities,
    val requestDeviceScanAccess: (onResult: (Boolean) -> Unit) -> Unit
)

/**
 * Платформенные адаптеры выбора и просмотра видео.
 *
 * Android сохраняет SAF, persistable URI permission и системный просмотрщик.
 * iOS предоставляет document picker и системный просмотр (этап iOS-адаптеров).
 */
@Composable
expect fun rememberVideoEnvironment(): VideoEnvironment
