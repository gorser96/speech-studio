@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.domain.usecase

import com.logoped_plus.domain.repository.GenerateChildLessonsResult
import com.logoped_plus.domain.repository.LessonRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class GenerateChildLessonsUseCase(
    private val lessonRepository: LessonRepository,
    private val clock: Clock = Clock.System
) {
    suspend operator fun invoke(childId: String): GenerateChildLessonsResult =
        lessonRepository.generateChildLessons(
            childId = childId,
            now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )
}
