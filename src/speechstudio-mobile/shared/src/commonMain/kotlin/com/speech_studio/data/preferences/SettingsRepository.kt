package com.speech_studio.data.preferences

import kotlinx.coroutines.flow.StateFlow

/**
 * Пользовательские настройки, общие для Android и iOS.
 *
 * Ключи и значения по умолчанию совпадают с прежним Android `SettingsStore`,
 * поэтому существующие установки читаются без миграции.
 */
interface SettingsRepository {
    val hideWeekend: StateFlow<Boolean>
    val gridStartHour: StateFlow<Int>
    val gridEndHour: StateFlow<Int>
    val visibleTagsLimit: StateFlow<Int>

    fun setHideWeekend(value: Boolean)
    fun setGridStartHour(value: Int)
    fun setGridEndHour(value: Int)
    fun setVisibleTagsLimit(value: Int)
}

internal object SettingsKeys {
    const val PREFS_FILE = "settings"
    const val KEY_HIDE_WEEKEND = "hide_weekend"
    const val DEFAULT_HIDE_WEEKEND = false
    const val KEY_GRID_START_HOUR = "grid_start_hour"
    const val DEFAULT_GRID_START_HOUR = 0
    const val KEY_GRID_END_HOUR = "grid_end_hour"
    const val DEFAULT_GRID_END_HOUR = 24
    const val KEY_VISIBLE_TAGS_LIMIT = "visible_tags_limit"
    const val DEFAULT_VISIBLE_TAGS_LIMIT = 3
}
