package com.logoped_plus.domain.repository

import com.logoped_plus.domain.model.WorkShift

sealed interface ShiftLoadState {
    val shifts: List<WorkShift>
    data class Loading(override val shifts: List<WorkShift> = emptyList()) : ShiftLoadState
    data class Ready(override val shifts: List<WorkShift>) : ShiftLoadState
    data class Error(override val shifts: List<WorkShift> = emptyList()) : ShiftLoadState
}