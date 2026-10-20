@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.logoped_plus.ui.screen.SettingsViewModel
import com.logoped_plus.ui.screen.SettingsViewModelFactory
import com.logoped_plus.ui.screen.children.ChildrenViewModel
import com.logoped_plus.ui.screen.children.ChildrenViewModelFactory
import com.logoped_plus.ui.screen.schedule.ScheduleViewModel
import com.logoped_plus.ui.screen.schedule.ScheduleViewModelFactory
import com.logoped_plus.ui.screen.tags.TagsViewModel
import com.logoped_plus.ui.screen.tags.TagsViewModelFactory

@Composable
fun App() {
    val context = LocalContext.current
    val container = (context.applicationContext as LogopedPlusApplication).container
    val lessonRepository = container.lessonRepository
    val childRepository = container.childRepository
    val tagRepository = container.tagRepository
    val settings = container.settings

    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(
            lessonRepository = lessonRepository,
            childRepository = childRepository,
            tagRepository = tagRepository,
            settings = settings,
            findLessonVideos = container.findLessonVideos
        )
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
    com.logoped_plus.ui.App(
        scheduleViewModel = scheduleViewModel,
        childrenViewModel = childrenViewModel,
        tagsViewModel = tagsViewModel,
        settingsViewModel = settingsViewModel,
        onExit = if (activity != null) ({ activity.finish() }) else null,
        showSectionBack = false
    )
}
