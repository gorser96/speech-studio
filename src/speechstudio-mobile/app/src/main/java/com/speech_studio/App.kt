@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speech_studio.ui.screen.SettingsViewModel
import com.speech_studio.ui.screen.SettingsViewModelFactory
import com.speech_studio.ui.screen.children.ChildrenViewModel
import com.speech_studio.ui.screen.children.ChildrenViewModelFactory
import com.speech_studio.ui.screen.schedule.ScheduleViewModel
import com.speech_studio.ui.screen.schedule.ScheduleViewModelFactory
import com.speech_studio.ui.screen.shift.ShiftViewModel
import com.speech_studio.ui.screen.shift.ShiftViewModelFactory
import com.speech_studio.ui.screen.shift.templates.ShiftTemplatesViewModel
import com.speech_studio.ui.screen.shift.templates.ShiftTemplatesViewModelFactory
import com.speech_studio.ui.screen.tags.TagsViewModel
import com.speech_studio.ui.screen.tags.TagsViewModelFactory

@Composable
fun App() {
    val context = LocalContext.current
    val container = (context.applicationContext as SpeechStudioApplication).container
    val lessonRepository = container.lessonRepository
    val childRepository = container.childRepository
    val tagRepository = container.tagRepository
    val shiftRepository = container.shiftRepository
    val settings = container.settings

    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(
            lessonRepository = lessonRepository,
            childRepository = childRepository,
            tagRepository = tagRepository,
            shiftRepository = shiftRepository,
            settings = settings,
            findLessonVideos = container.findLessonVideos
        )
    )

    val shiftViewModel: ShiftViewModel = viewModel(
        factory = ShiftViewModelFactory(
            shiftRepository = shiftRepository,
            childRepository = childRepository,
            lessonRepository = lessonRepository,
            tagRepository = tagRepository,
            findShiftVideos = container.findShiftVideos
        )
    )

    val shiftTemplatesViewModel: ShiftTemplatesViewModel = viewModel(
        factory = ShiftTemplatesViewModelFactory(shiftRepository = shiftRepository)
    )

    val childrenViewModel: ChildrenViewModel = viewModel(
        factory = ChildrenViewModelFactory(
            childRepository = childRepository,
            tagRepository = tagRepository,
            generateChildLessons = container.generateChildLessons,
            settings = settings
        )
    )

    val tagsViewModel: TagsViewModel = viewModel(
        factory = TagsViewModelFactory(
            tagRepository = tagRepository
        )
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            settings = settings
        )
    )

    val activity = context as? Activity
    com.speech_studio.ui.App(
        scheduleViewModel = scheduleViewModel,
        childrenViewModel = childrenViewModel,
        tagsViewModel = tagsViewModel,
        settingsViewModel = settingsViewModel,
        shiftViewModel = shiftViewModel,
        shiftTemplatesViewModel = shiftTemplatesViewModel,
        onExit = if (activity != null) ({ activity.finish() }) else null,
        showSectionBack = false
    )
}
