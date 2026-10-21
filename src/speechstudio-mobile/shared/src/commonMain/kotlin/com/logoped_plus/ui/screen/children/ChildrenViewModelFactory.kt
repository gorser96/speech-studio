@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ui.screen.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.logoped_plus.data.preferences.SettingsRepository
import com.logoped_plus.domain.repository.ChildRepository
import com.logoped_plus.domain.repository.TagRepository
import com.logoped_plus.domain.usecase.GenerateChildLessonsUseCase
import kotlin.reflect.KClass

class ChildrenViewModelFactory(
    private val childRepository: ChildRepository,
    private val tagRepository: TagRepository,
    private val generateChildLessons: GenerateChildLessonsUseCase,
    private val settings: SettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        if (modelClass == ChildrenViewModel::class) {
            return ChildrenViewModel(
                childRepository = childRepository,
                tagRepository = tagRepository,
                generateChildLessons = generateChildLessons,
                settings = settings
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.simpleName}"
        )
    }
}
