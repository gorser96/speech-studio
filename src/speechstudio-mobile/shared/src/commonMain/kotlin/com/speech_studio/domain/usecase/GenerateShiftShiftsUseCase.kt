@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio.domain.usecase

import com.speech_studio.domain.repository.ShiftGenerationResult
import com.speech_studio.domain.repository.ShiftRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class GenerateShiftShiftsUseCase(
    private val shiftRepository: ShiftRepository,
    private val clock: Clock = Clock.System
) {
    suspend operator fun invoke(templateId: String): ShiftGenerationResult =
        shiftRepository.generateShifts(
            templateId = templateId,
            today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        )
}