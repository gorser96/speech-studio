package com.speech_studio.platform

import androidx.compose.runtime.Composable

/**
 * Перехват системного возврата.
 *
 * Android использует прежний обработчик, iOS не перехватывает системный жест:
 * вложенные экраны показывают экранное действие «Назад».
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
