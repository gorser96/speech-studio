package com.speech_studio

import android.app.Application
import com.speech_studio.app.AppContainer

class SpeechStudioApplication : Application() {
    val container: AppContainer by lazy { createAppContainer(this) }
}
