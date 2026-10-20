package com.logoped_plus.domain.repository

import com.logoped_plus.domain.model.Lesson

sealed interface LessonWriteResult {
    data class Success(val lesson: Lesson) : LessonWriteResult
    data class Failure(val reason: Reason) : LessonWriteResult
    enum class Reason { NotReady, InvalidData, UnknownChild, NotFound, Conflict, StorageUnavailable }
}
