package com.speech_studio.domain.repository

import com.speech_studio.domain.model.Lesson

sealed interface LessonLoadState {
    val lessons: List<Lesson>
    data class Loading(override val lessons: List<Lesson> = emptyList()) : LessonLoadState
    data class Ready(override val lessons: List<Lesson>) : LessonLoadState
    data class Error(override val lessons: List<Lesson> = emptyList()) : LessonLoadState
}
