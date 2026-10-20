package com.logoped_plus.domain.repository

sealed interface TagWriteResult {
    data object Success : TagWriteResult
    data class Failure(val reason: Reason) : TagWriteResult
    enum class Reason { InvalidName, DuplicateName, NotFound, StorageUnavailable, NotReady }
}
