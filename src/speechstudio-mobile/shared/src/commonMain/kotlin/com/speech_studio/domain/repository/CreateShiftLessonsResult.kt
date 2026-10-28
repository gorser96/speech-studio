package com.speech_studio.domain.repository

sealed interface CreateShiftLessonsResult {
    /** [created] — создано новых занятий, [linked] — связано существующих. */
    data class Success(val created: Int, val linked: Int) : CreateShiftLessonsResult
    data class Failure(val reason: Reason) : CreateShiftLessonsResult
    enum class Reason { NotReady, NotFound, Conflict, StorageUnavailable }
}