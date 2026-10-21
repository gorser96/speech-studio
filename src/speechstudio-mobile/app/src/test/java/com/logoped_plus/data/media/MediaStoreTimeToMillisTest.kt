package com.logoped_plus.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreTimeToMillisTest {

    @Test
    fun secondsAreConvertedToMillis() {
        // OnePlus PJE110, DATE_ADDED=1790766984 (2026-09-30 14:16 MSK).
        assertEquals(1_790_766_984_000L, mediaStoreTimeToMillis(1_790_766_984L))
        assertEquals(0L, mediaStoreTimeToMillis(0L))
    }

    @Test
    fun millisAreKeptAsIs() {
        // Same device, DATE_TAKEN=1790766989000 is already milliseconds.
        assertEquals(1_790_766_989_000L, mediaStoreTimeToMillis(1_790_766_989_000L))
    }

    @Test
    fun thresholdSeparatesUnits() {
        assertEquals(99_999_999_999_000L, mediaStoreTimeToMillis(99_999_999_999L))
        assertEquals(100_000_000_000L, mediaStoreTimeToMillis(100_000_000_000L))
    }
}
