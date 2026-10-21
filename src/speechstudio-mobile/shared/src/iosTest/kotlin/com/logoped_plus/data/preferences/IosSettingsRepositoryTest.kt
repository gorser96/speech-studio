package com.logoped_plus.data.preferences

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Проверяет, что iOS-настройки читаются из NSUserDefaults после создания репозитория заново.
 */
class IosSettingsRepositoryTest {

    @Test
    fun settingsSurviveRepositoryRecreation() {
        val suite = "com.logopedplus.test.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val first = IosSettingsRepository(defaults)
            assertFalse(first.hideWeekend.value)
            assertEquals(SettingsKeys.DEFAULT_GRID_START_HOUR, first.gridStartHour.value)
            assertEquals(SettingsKeys.DEFAULT_GRID_END_HOUR, first.gridEndHour.value)
            assertEquals(SettingsKeys.DEFAULT_VISIBLE_TAGS_LIMIT, first.visibleTagsLimit.value)

            first.setHideWeekend(true)
            first.setGridStartHour(8)
            first.setGridEndHour(21)
            first.setVisibleTagsLimit(5)
            defaults.synchronize()

            val reopened = IosSettingsRepository(NSUserDefaults(suiteName = suite))
            assertTrue(reopened.hideWeekend.value)
            assertEquals(8, reopened.gridStartHour.value)
            assertEquals(21, reopened.gridEndHour.value)
            assertEquals(5, reopened.visibleTagsLimit.value)
        } finally {
            defaults.removePersistentDomainForName(suite)
        }
    }
}
