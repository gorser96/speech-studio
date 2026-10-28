@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio

import android.content.Context
import com.speech_studio.app.AppContainer
import com.speech_studio.data.local.createChildrenDatabase
import com.speech_studio.data.media.MediaStoreVideoScanner
import com.speech_studio.data.preferences.createSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Android-фабрика общего контейнера.
 *
 * Использует прежний файл `children.db` и системный SQLite, а также MediaStore
 * для автопоиска видео.
 */
fun createAppContainer(
    context: Context,
    databaseName: String = "children.db"
): AppContainer = AppContainer(
    database = createChildrenDatabase(context, databaseName),
    settings = createSettingsRepository(context),
    videoLibrary = MediaStoreVideoScanner(context),
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
)
