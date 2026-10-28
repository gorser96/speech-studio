package com.speech_studio.platform

import java.util.UUID

actual fun randomUuidString(): String = UUID.randomUUID().toString()
