@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio.domain.usecase

import com.speech_studio.domain.repository.GenerateChildLessonsResult
import com.speech_studio.domain.repository.LessonRepository
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
