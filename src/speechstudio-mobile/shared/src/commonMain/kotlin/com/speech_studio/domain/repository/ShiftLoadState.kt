package com.speech_studio.domain.repository

import com.speech_studio.domain.model.WorkShift

sealed interface ShiftLoadState {
    val shifts: List<WorkShift>
    data class Loading(override val shifts: List<WorkShift> = emptyList()) : ShiftLoadState
    data class Ready(override val shifts: List<WorkShift>) : ShiftLoadState
    data class Error(override val shifts: List<WorkShift> = emptyList()) : ShiftLoadState
}