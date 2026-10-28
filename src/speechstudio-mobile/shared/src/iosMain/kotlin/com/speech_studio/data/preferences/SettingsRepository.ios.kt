package com.speech_studio.data.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

/**
 * iOS-реализация использует те же ключи и значения по умолчанию, что и Android.
 */
class IosSettingsRepository(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : SettingsRepository {
    private val mutableHideWeekend = MutableStateFlow(
        defaults.boolForKey(SettingsKeys.KEY_HIDE_WEEKEND)
    )
    override val hideWeekend: StateFlow<Boolean> = mutableHideWeekend.asStateFlow()

    private val mutableGridStartHour = MutableStateFlow(
        intOrDefault(SettingsKeys.KEY_GRID_START_HOUR, SettingsKeys.DEFAULT_GRID_START_HOUR)
    )
    override val gridStartHour: StateFlow<Int> = mutableGridStartHour.asStateFlow()

    private val mutableGridEndHour = MutableStateFlow(
        intOrDefault(SettingsKeys.KEY_GRID_END_HOUR, SettingsKeys.DEFAULT_GRID_END_HOUR)
    )
    override val gridEndHour: StateFlow<Int> = mutableGridEndHour.asStateFlow()

    private val mutableVisibleTagsLimit = MutableStateFlow(
        intOrDefault(SettingsKeys.KEY_VISIBLE_TAGS_LIMIT, SettingsKeys.DEFAULT_VISIBLE_TAGS_LIMIT)
    )
    override val visibleTagsLimit: StateFlow<Int> = mutableVisibleTagsLimit.asStateFlow()

    override fun setHideWeekend(value: Boolean) {
        defaults.setBool(value, forKey = SettingsKeys.KEY_HIDE_WEEKEND)
        mutableHideWeekend.value = value
    }

    override fun setGridStartHour(value: Int) {
        defaults.setInteger(value.toLong(), forKey = SettingsKeys.KEY_GRID_START_HOUR)
        mutableGridStartHour.value = value
    }

    override fun setGridEndHour(value: Int) {
        defaults.setInteger(value.toLong(), forKey = SettingsKeys.KEY_GRID_END_HOUR)
        mutableGridEndHour.value = value
    }

    override fun setVisibleTagsLimit(value: Int) {
        defaults.setInteger(value.toLong(), forKey = SettingsKeys.KEY_VISIBLE_TAGS_LIMIT)
        mutableVisibleTagsLimit.value = value
    }

    private fun intOrDefault(key: String, default: Int): Int =
        if (defaults.objectForKey(key) == null) default else defaults.integerForKey(key).toInt()
}

fun createSettingsRepository(): SettingsRepository = IosSettingsRepository()
