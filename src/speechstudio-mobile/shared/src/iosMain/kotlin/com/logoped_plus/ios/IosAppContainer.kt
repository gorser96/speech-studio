@file:OptIn(ExperimentalForeignApi::class, kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ios

import com.logoped_plus.app.AppContainer
import com.logoped_plus.data.local.createChildrenDatabase
import com.logoped_plus.data.preferences.createSettingsRepository
import com.logoped_plus.domain.model.DeviceVideo
import com.logoped_plus.domain.repository.VideoLibrary
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import kotlin.time.Instant

/**
 * iOS-фабрика общего контейнера.
 *
 * БД `children.db` создаётся в Application Support приложения и использует
 * bundled SQLite; настройки читаются из `NSUserDefaults`. Автопоиск видео
 * по медиатеке на iOS не поддерживается, поэтому источник пуст.
 */
internal fun createIosAppContainer(): AppContainer {
    val supportDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    ) ?: error("Не удалось открыть каталог Application Support")
    val databaseUrl = supportDirectory.URLByAppendingPathComponent("children.db")
        ?: error("Не удалось получить путь к children.db")
    val databasePath = databaseUrl.path
        ?: error("Не удалось получить путь к children.db")

    return AppContainer(
        database = createChildrenDatabase(databasePath),
        settings = createSettingsRepository(),
        videoLibrary = EmptyVideoLibrary
    )
}

private object EmptyVideoLibrary : VideoLibrary {
    override suspend fun findVideos(from: Instant, to: Instant): List<DeviceVideo> = emptyList()
}
