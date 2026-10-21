package com.logoped_plus.domain.repository

import com.logoped_plus.domain.model.Lesson

sealed interface LessonLoadState {
    val lessons: List<Lesson>
    data class Loading(override val lessons: List<Lesson> = emptyList()) : LessonLoadState
    data class Ready(override val lessons: List<Lesson>) : LessonLoadState
    data class Error(override val lessons: List<Lesson> = emptyList()) : LessonLoadState
}
