package com.logoped_plus.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.logoped_plus.platform.PlatformBackHandler
import com.logoped_plus.ui.screen.ChildrenScreen
import com.logoped_plus.ui.screen.ScheduleScreen
import com.logoped_plus.ui.screen.SettingsScreen
import com.logoped_plus.ui.screen.SettingsViewModel
import com.logoped_plus.ui.screen.TagsScreen
import com.logoped_plus.ui.screen.children.ChildrenViewModel
import com.logoped_plus.ui.screen.schedule.LessonEditorMode
import com.logoped_plus.ui.screen.schedule.ScheduleAction
import com.logoped_plus.ui.screen.schedule.ScheduleViewModel
import com.logoped_plus.ui.screen.tags.TagsViewModel
import kotlinx.coroutines.launch
import speechstudio.shared.generated.resources.Res
import speechstudio.shared.generated.resources.icon1024
import org.jetbrains.compose.resources.painterResource

private val AppScreenSaver: Saver<AppScreen, String> = Saver(
    save = { it.name },
    restore = { name -> AppScreen.entries.first { it.name == name } }
)

/**
 * Общая оболочка приложения: ручная навигация, drawer и подтверждение выхода.
 *
 * [logo] и [onExit] внедряет платформенная оболочка, чтобы общий код не ссылался
 * на Android-ресурсы и `Activity`. [showSectionBack] включает экранное действие
 * «Назад» для корневых разделов на iOS, где системный жест не перехватывается.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    scheduleViewModel: ScheduleViewModel,
    childrenViewModel: ChildrenViewModel,
    tagsViewModel: TagsViewModel,
    settingsViewModel: SettingsViewModel,
    logo: Painter? = null,
    onExit: (() -> Unit)? = null,
    showSectionBack: Boolean = false
) {
    val scheduleState by scheduleViewModel.uiState.collectAsState()
    val childrenState by childrenViewModel.uiState.collectAsState()
    val tagsState by tagsViewModel.uiState.collectAsState()
    val appLogo = logo ?: painterResource(Res.drawable.icon1024)
    val appBusy = scheduleState.saving || childrenState.busy || tagsState.busy
    var currentScreen by rememberSaveable(stateSaver = AppScreenSaver) { mutableStateOf(AppScreen.Schedule) }
    var showExitConfirmation by rememberSaveable {
        mutableStateOf(false)
    }

    PlatformBackHandler(
        enabled = currentScreen != AppScreen.Schedule
    ) {
        if (!appBusy) currentScreen = AppScreen.Schedule
    }
    PlatformBackHandler(
        enabled = currentScreen == AppScreen.Schedule && onExit != null
    ) {
        if (!appBusy) showExitConfirmation = true
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentCardId = childrenState.cardId
    val cardOpen = currentScreen == AppScreen.Children && currentCardId != null
    val lessonEditor = scheduleState.editor
    val lessonOpen = currentScreen == AppScreen.Schedule && lessonEditor != null
    val sectionBackVisible = showSectionBack && currentScreen != AppScreen.Schedule
    val screenTitle = when (currentScreen) {
        AppScreen.Schedule -> "Расписание"
        AppScreen.Children -> "Дети"
        AppScreen.Tags -> "Тэги"
        AppScreen.Settings -> "Настройки"
    }

    ModalNavigationDrawer(
        gesturesEnabled = !appBusy,
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                enabled = !appBusy,
                currentScreen = currentScreen,
                logo = appLogo,
                onScreenSelected = { screen ->
                    if (!appBusy) currentScreen = screen

                    scope.launch {
                        if (!appBusy) drawerState.close()
                    }
                }
            )
        }
    ) {

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        when {
                            cardOpen -> IconButton(enabled = !appBusy,
                                onClick = {
                                    if (!appBusy) childrenViewModel.closeCard()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "К списку детей"
                                )
                            }

                            lessonOpen -> IconButton(enabled = !appBusy,
                                onClick = {
                                    if (!appBusy) scheduleViewModel.onAction(when (lessonEditor?.mode) {
                                        LessonEditorMode.CREATE -> ScheduleAction.CancelCreatingLesson
                                        LessonEditorMode.EDIT -> ScheduleAction.CancelEditingLesson
                                        else -> ScheduleAction.CloseLesson
                                    })
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Назад"
                                )
                            }

                            sectionBackVisible -> IconButton(enabled = !appBusy,
                                onClick = {
                                    if (!appBusy) currentScreen = AppScreen.Schedule
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "К расписанию"
                                )
                            }

                            else -> IconButton(enabled = !appBusy,
                                onClick = {
                                    scope.launch {
                                        if (!appBusy) drawerState.open()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Открыть меню"
                                )
                            }
                        }
                    },
                    title = {
                        Text(screenTitle)
                    }
                )
            }
        ) { innerPadding ->

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.Schedule -> ScheduleScreen(
                        viewModel = scheduleViewModel
                    )

                    AppScreen.Children -> ChildrenScreen(
                        viewModel = childrenViewModel
                    )

                    AppScreen.Tags -> TagsScreen(
                        viewModel = tagsViewModel
                    )

                    AppScreen.Settings -> SettingsScreen(
                        viewModel = settingsViewModel
                    )
                }
            }
        }
    }

    val exit = onExit
    if (showExitConfirmation && exit != null) {
        AlertDialog(
            onDismissRequest = {
                showExitConfirmation = false
            },
            title = {
                Text("Выйти из приложения?")
            },
            text = {
                Text("Вы действительно хотите выйти?")
            },
            confirmButton = {
                TextButton(
                    enabled = !appBusy,
                    onClick = {
                        if (!appBusy) {
                            showExitConfirmation = false
                            exit()
                        }
                    }
                ) {
                    Text("Выйти")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}
