package com.speech_studio.domain.repository

import com.speech_studio.domain.model.Lesson
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDateTime

interface LessonRepository {

    val state: StateFlow<LessonLoadState>
    fun retryLoading()
    suspend fun addLesson(lesson: Lesson): LessonWriteResult
    suspend fun updateLesson(lesson: Lesson): LessonWriteResult
    suspend fun deleteLesson(id: String): LessonWriteResult
    suspend fun generateChildLessons(childId: String, now: LocalDateTime): GenerateChildLessonsResult
}
