package com.speech_studio.domain.repository

sealed interface ShiftGenerationResult {
    /** [created] — создано смен, [existing] — пропущено уже существующих. */
    data class Success(val created: Int, val existing: Int) : ShiftGenerationResult
    data class Failure(val reason: Reason) : ShiftGenerationResult
    enum class Reason { NotReady, InvalidTemplate, TemplateNotFound, StorageUnavailable }
}