package com.speech_studio.domain.model

data class LessonSpan(
    val epochDay: Long,
    val nanoOfDay: Long,
    val durationMinutes: Int
)
