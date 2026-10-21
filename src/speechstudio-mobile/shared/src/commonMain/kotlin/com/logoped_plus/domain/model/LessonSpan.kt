package com.logoped_plus.domain.model

data class LessonSpan(
    val epochDay: Long,
    val nanoOfDay: Long,
    val durationMinutes: Int
)
