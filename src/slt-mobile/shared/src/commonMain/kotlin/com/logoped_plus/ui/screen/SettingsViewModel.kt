package com.logoped_plus.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logoped_plus.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val hideWeekend: Boolean = false,
    val gridStartHour: Int = 0,
    val gridEndHour: Int = 24,
    val visibleTagsLimit: Int = 3
) {
    val visibleTagsLimitInvalid: Boolean get() = visibleTagsLimit !in 0..10
}

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState(
        hideWeekend = settings.hideWeekend.value,
        gridStartHour = settings.gridStartHour.value,
        gridEndHour = settings.gridEndHour.value,
        visibleTagsLimit = settings.visibleTagsLimit.value
    ))
    val uiState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.hideWeekend.collect { hidden -> mutableState.update { it.copy(hideWeekend = hidden) } }
        }
        viewModelScope.launch {
            settings.gridStartHour.collect { hour -> mutableState.update { it.copy(gridStartHour = hour) } }
        }
        viewModelScope.launch {
            settings.gridEndHour.collect { hour -> mutableState.update { it.copy(gridEndHour = hour) } }
        }
        viewModelScope.launch {
            settings.visibleTagsLimit.collect { limit -> mutableState.update { it.copy(visibleTagsLimit = limit) } }
        }
    }

    fun setHideWeekend(value: Boolean) {
        settings.setHideWeekend(value)
    }

    fun setGridStartHour(value: Int) {
        settings.setGridStartHour(value)
    }

    fun setGridEndHour(value: Int) {
        settings.setGridEndHour(value)
    }

    fun setVisibleTagsLimit(value: Int) {
        settings.setVisibleTagsLimit(value)
    }
}
