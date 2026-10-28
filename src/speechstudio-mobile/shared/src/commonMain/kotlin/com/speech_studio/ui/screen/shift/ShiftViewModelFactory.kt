@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio.ui.screen.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.speech_studio.domain.repository.ChildRepository
import com.speech_studio.domain.repository.LessonRepository
import com.speech_studio.domain.repository.ShiftRepository
import com.speech_studio.domain.repository.TagRepository
import com.speech_studio.domain.usecase.FindShiftVideosUseCase
import kotlin.reflect.KClass

class ShiftViewModelFactory(
    private val shiftRepository: ShiftRepository,
    private val childRepository: ChildRepository,
    private val lessonRepository: LessonRepository,
    private val tagRepository: TagRepository,
    private val findShiftVideos: FindShiftVideosUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        if (modelClass == ShiftViewModel::class) {
            return ShiftViewModel(shiftRepository, childRepository, lessonRepository, tagRepository, findShiftVideos) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.simpleName}"
        )
    }
}