package com.logoped_plus.data.preferences

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidSettingsRepositoryTest {

    @Test
    fun readsLegacyKeysAndPersistsChangesToSameFile() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        preferences.edit()
            .putBoolean("hide_weekend", true)
            .putInt("grid_start_hour", 8)
            .putInt("grid_end_hour", 20)
            .putInt("visible_tags_limit", 5)
            .commit()

        val repository = AndroidSettingsRepository(context)
        assertTrue(repository.hideWeekend.value)
        assertEquals(8, repository.gridStartHour.value)
        assertEquals(20, repository.gridEndHour.value)
        assertEquals(5, repository.visibleTagsLimit.value)

        repository.setHideWeekend(false)
        repository.setGridStartHour(9)
        repository.setGridEndHour(19)
        repository.setVisibleTagsLimit(2)

        val reloaded = AndroidSettingsRepository(context)
        assertFalse(reloaded.hideWeekend.value)
        assertEquals(9, reloaded.gridStartHour.value)
        assertEquals(19, reloaded.gridEndHour.value)
        assertEquals(2, reloaded.visibleTagsLimit.value)
        assertEquals(9, preferences.getInt("grid_start_hour", -1))
        assertEquals(2, preferences.getInt("visible_tags_limit", -1))
    }
}
