package com.speech_studio.domain.repository

sealed interface ShiftWriteResult {
    data object Success : ShiftWriteResult
    data class Failure(val reason: Reason) : ShiftWriteResult
    enum class Reason { NotReady, InvalidData, NotFound, UnknownChild, StorageUnavailable }
}