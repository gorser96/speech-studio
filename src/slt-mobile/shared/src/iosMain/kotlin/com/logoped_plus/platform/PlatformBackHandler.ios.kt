package com.logoped_plus.platform

import androidx.compose.runtime.Composable

/**
 * На iOS системный жест возврата не перехватывается: приложение уходит в фон,
 * а навигация внутри приложения выполняется экранным действием «Назад».
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op by design.
}
