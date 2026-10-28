package com.speech_studio.domain.repository

import com.speech_studio.domain.model.GenerationCounts

sealed interface GenerateChildLessonsResult {
    data class Success(val counts: GenerationCounts) : GenerateChildLessonsResult
    data class Failure(val reason: Reason) : GenerateChildLessonsResult
    enum class Reason { NotReady, NotFound, InvalidSchedule, StorageUnavailable }
}
