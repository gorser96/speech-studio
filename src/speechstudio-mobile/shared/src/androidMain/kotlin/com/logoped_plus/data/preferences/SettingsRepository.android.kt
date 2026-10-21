package com.logoped_plus.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android-реализация сохраняет прежний файл `settings` и прежние ключи.
 */
class AndroidSettingsRepository(context: Context) : SettingsRepository {
    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(SettingsKeys.PREFS_FILE, Context.MODE_PRIVATE)

    private val mutableHideWeekend = MutableStateFlow(
        preferences.getBoolean(SettingsKeys.KEY_HIDE_WEEKEND, SettingsKeys.DEFAULT_HIDE_WEEKEND)
    )
    override val hideWeekend: StateFlow<Boolean> = mutableHideWeekend.asStateFlow()

    private val mutableGridStartHour = MutableStateFlow(
        preferences.getInt(SettingsKeys.KEY_GRID_START_HOUR, SettingsKeys.DEFAULT_GRID_START_HOUR)
    )
    override val gridStartHour: StateFlow<Int> = mutableGridStartHour.asStateFlow()

    private val mutableGridEndHour = MutableStateFlow(
        preferences.getInt(SettingsKeys.KEY_GRID_END_HOUR, SettingsKeys.DEFAULT_GRID_END_HOUR)
    )
    override val gridEndHour: StateFlow<Int> = mutableGridEndHour.asStateFlow()

    private val mutableVisibleTagsLimit = MutableStateFlow(
        preferences.getInt(SettingsKeys.KEY_VISIBLE_TAGS_LIMIT, SettingsKeys.DEFAULT_VISIBLE_TAGS_LIMIT)
    )
    override val visibleTagsLimit: StateFlow<Int> = mutableVisibleTagsLimit.asStateFlow()

    override fun setHideWeekend(value: Boolean) {
        preferences.edit().putBoolean(SettingsKeys.KEY_HIDE_WEEKEND, value).apply()
        mutableHideWeekend.value = value
    }

    override fun setGridStartHour(value: Int) {
        preferences.edit().putInt(SettingsKeys.KEY_GRID_START_HOUR, value).apply()
        mutableGridStartHour.value = value
    }

    override fun setGridEndHour(value: Int) {
        preferences.edit().putInt(SettingsKeys.KEY_GRID_END_HOUR, value).apply()
        mutableGridEndHour.value = value
    }

    override fun setVisibleTagsLimit(value: Int) {
        preferences.edit().putInt(SettingsKeys.KEY_VISIBLE_TAGS_LIMIT, value).apply()
        mutableVisibleTagsLimit.value = value
    }
}

fun createSettingsRepository(context: Context): SettingsRepository = AndroidSettingsRepository(context)
