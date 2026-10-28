package com.speech_studio.ios

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speech_studio.app.AppContainer
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

/**
 * iOS-оболочка общего приложения.
 *
 * Выход выполняет система, поэтому `onExit = null`; подтверждение выхода не
 * показывается. `showSectionBack = true` добавляет экранное действие «Назад»
 * для вложенных экранов и корневых разделов.
 */
@Composable
fun IosApp(container: AppContainer) {
    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(
            lessonRepository = container.lessonRepository,
            childRepository = container.childRepository,
            tagRepository = container.tagRepository,
            shiftRepository = container.shiftRepository,
            settings = container.settings,
            findLessonVideos = container.findLessonVideos
        )
    )

    val shiftViewModel: ShiftViewModel = viewModel(
        factory = ShiftViewModelFactory(
            shiftRepository = container.shiftRepository,
            childRepository = container.childRepository,
            lessonRepository = container.lessonRepository,
            tagRepository = container.tagRepository,
            findShiftVideos = container.findShiftVideos
        )
    )

    val childrenViewModel: ChildrenViewModel = viewModel(
        factory = ChildrenViewModelFactory(
            childRepository = container.childRepository,
            tagRepository = container.tagRepository,
            generateChildLessons = container.generateChildLessons,
            settings = container.settings
        )
    )

    val shiftTemplatesViewModel: ShiftTemplatesViewModel = viewModel(
        factory = ShiftTemplatesViewModelFactory(shiftRepository = container.shiftRepository)
    )

    val tagsViewModel: TagsViewModel = viewModel(
        factory = TagsViewModelFactory(
            tagRepository = container.tagRepository
        )
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            settings = container.settings
        )
    )

    com.speech_studio.ui.App(
        scheduleViewModel = scheduleViewModel,
        childrenViewModel = childrenViewModel,
        tagsViewModel = tagsViewModel,
        settingsViewModel = settingsViewModel,
        shiftViewModel = shiftViewModel,
        shiftTemplatesViewModel = shiftTemplatesViewModel,
        onExit = null,
        showSectionBack = true
    )
}
