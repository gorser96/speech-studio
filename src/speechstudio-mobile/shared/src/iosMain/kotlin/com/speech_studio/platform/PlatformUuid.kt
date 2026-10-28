package com.speech_studio.platform

import platform.Foundation.NSUUID

actual fun randomUuidString(): String = NSUUID().UUIDString.lowercase()
