@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ui.screen.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.logoped_plus.data.preferences.SettingsRepository
import com.logoped_plus.domain.repository.ChildRepository
import com.logoped_plus.domain.repository.LessonRepository
import com.logoped_plus.domain.repository.ShiftRepository
import com.logoped_plus.domain.repository.TagRepository
import com.logoped_plus.domain.usecase.FindLessonVideosUseCase
import kotlin.reflect.KClass

class ScheduleViewModelFactory(
    private val lessonRepository: LessonRepository,
    private val childRepository: ChildRepository,
    private val tagRepository: TagRepository,
    private val shiftRepository: ShiftRepository,
    private val settings: SettingsRepository,
    private val findLessonVideos: FindLessonVideosUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        if (modelClass == ScheduleViewModel::class) {
            return ScheduleViewModel(lessonRepository, childRepository, tagRepository, shiftRepository, settings, findLessonVideos) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.simpleName}"
        )
    }
}
