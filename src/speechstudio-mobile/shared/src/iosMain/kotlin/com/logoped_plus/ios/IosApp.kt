package com.logoped_plus.ios

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.logoped_plus.app.AppContainer
import com.logoped_plus.ui.screen.SettingsViewModel
import com.logoped_plus.ui.screen.SettingsViewModelFactory
import com.logoped_plus.ui.screen.children.ChildrenViewModel
import com.logoped_plus.ui.screen.children.ChildrenViewModelFactory
import com.logoped_plus.ui.screen.schedule.ScheduleViewModel
import com.logoped_plus.ui.screen.schedule.ScheduleViewModelFactory
import com.logoped_plus.ui.screen.tags.TagsViewModel
import com.logoped_plus.ui.screen.tags.TagsViewModelFactory

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
            settings = container.settings,
            findLessonVideos = container.findLessonVideos
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

    com.logoped_plus.ui.App(
        scheduleViewModel = scheduleViewModel,
        childrenViewModel = childrenViewModel,
        tagsViewModel = tagsViewModel,
        settingsViewModel = settingsViewModel,
        onExit = null,
        showSectionBack = true
    )
}
