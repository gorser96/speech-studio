package com.logoped_plus

import android.app.Application
import com.logoped_plus.app.AppContainer

class LogopedPlusApplication : Application() {
    val container: AppContainer by lazy { createAppContainer(this) }
}
