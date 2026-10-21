package com.logoped_plus.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.logoped_plus.data.preferences.SettingsRepository
import kotlin.reflect.KClass

class SettingsViewModelFactory(
    private val settings: SettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        if (modelClass == SettingsViewModel::class) {
            return SettingsViewModel(
                settings = settings
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.simpleName}"
        )
    }
}
