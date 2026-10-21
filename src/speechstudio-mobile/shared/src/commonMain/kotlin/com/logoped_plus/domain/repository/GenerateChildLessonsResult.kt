package com.logoped_plus.domain.repository

import com.logoped_plus.domain.model.GenerationCounts

sealed interface GenerateChildLessonsResult {
    data class Success(val counts: GenerationCounts) : GenerateChildLessonsResult
    data class Failure(val reason: Reason) : GenerateChildLessonsResult
    enum class Reason { NotReady, NotFound, InvalidSchedule, StorageUnavailable }
}
