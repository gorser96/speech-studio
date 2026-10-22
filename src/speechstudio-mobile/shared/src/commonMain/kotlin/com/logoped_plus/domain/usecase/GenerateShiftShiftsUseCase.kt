@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.domain.usecase

import com.logoped_plus.domain.repository.ShiftGenerationResult
import com.logoped_plus.domain.repository.ShiftRepository
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