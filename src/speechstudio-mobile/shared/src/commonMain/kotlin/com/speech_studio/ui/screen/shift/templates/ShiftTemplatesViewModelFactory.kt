@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio.ui.screen.shift.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.speech_studio.domain.repository.ShiftRepository
import kotlin.reflect.KClass

class ShiftTemplatesViewModelFactory(
    private val shiftRepository: ShiftRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        if (modelClass == ShiftTemplatesViewModel::class) {
            return ShiftTemplatesViewModel(shiftRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
    }
}
