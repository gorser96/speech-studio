package com.logoped_plus

import android.app.Application
import com.logoped_plus.app.AppContainer

class SpeechStudioApplication : Application() {
    val container: AppContainer by lazy { createAppContainer(this) }
}
