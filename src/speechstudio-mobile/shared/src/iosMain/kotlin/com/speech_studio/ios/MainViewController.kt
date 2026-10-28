package com.speech_studio.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.speech_studio.ui.theme.SpeechStudioTheme
import platform.UIKit.UIViewController

private val container by lazy { createIosAppContainer() }

/**
 * Точка входа iOS: Compose-контроллер поверх общего `App`.
 *
 * Контейнер живёт столько же, сколько процесс приложения.
 */
@Suppress("FunctionName")
fun MainViewController(): UIViewController = ComposeUIViewController {
    SpeechStudioTheme {
        IosApp(container)
    }
}
