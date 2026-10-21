@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus

import android.content.Context
import com.logoped_plus.app.AppContainer
import com.logoped_plus.data.local.createChildrenDatabase
import com.logoped_plus.data.media.MediaStoreVideoScanner
import com.logoped_plus.data.preferences.createSettingsRepository
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
