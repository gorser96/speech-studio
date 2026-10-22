@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ui.screen.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.logoped_plus.domain.repository.ChildRepository
import com.logoped_plus.domain.repository.LessonRepository
import com.logoped_plus.domain.repository.ShiftRepository
import com.logoped_plus.domain.repository.TagRepository
import com.logoped_plus.domain.usecase.FindShiftVideosUseCase
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