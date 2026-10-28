@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio.app

import com.speech_studio.data.local.ChildrenDatabase
import com.speech_studio.data.preferences.SettingsRepository
import com.speech_studio.data.repository.RoomChildRepository
import com.speech_studio.data.repository.RoomLessonRepository
import com.speech_studio.data.repository.RoomShiftRepository
import com.speech_studio.data.repository.RoomTagRepository
import com.speech_studio.domain.repository.VideoLibrary
import com.speech_studio.domain.usecase.FindLessonVideosUseCase
import com.speech_studio.domain.usecase.FindShiftVideosUseCase
import com.speech_studio.domain.usecase.GenerateChildLessonsUseCase
import com.speech_studio.domain.usecase.GenerateShiftShiftsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.time.Clock

/**
 * Общий контейнер приложения: репозитории, use case и настройки.
 *
 * Платформенные оболочки передают готовую БД, хранилище настроек и источник
 * видео устройства, поэтому Android и iOS используют одни и те же зависимости.
 */
class AppContainer(
    private val database: ChildrenDatabase,
    val settings: SettingsRepository,
    videoLibrary: VideoLibrary,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    clock: Clock = Clock.System
) : AutoCloseable {
    val childRepository = RoomChildRepository(database.childDao(), scope)
    val lessonRepository = RoomLessonRepository(database.lessonDao(), childRepository, scope)
    val tagRepository = RoomTagRepository(database.tagDao(), scope)
    val shiftRepository = RoomShiftRepository(database.shiftDao(), scope)
    val generateChildLessons = GenerateChildLessonsUseCase(lessonRepository, clock)
    val findLessonVideos = FindLessonVideosUseCase(videoLibrary)
    val findShiftVideos = FindShiftVideosUseCase(videoLibrary)
    val generateShifts = GenerateShiftShiftsUseCase(shiftRepository, clock)

    override fun close() {
        scope.cancel()
        database.close()
    }
}
