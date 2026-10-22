@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.domain.usecase

import com.logoped_plus.domain.model.DeviceVideo
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShiftVideoMatcherTest {

    private val zone = TimeZone.UTC

    private fun lesson(id: String, hour: Int, minute: Int, duration: Int = 30) =
        ShiftLessonVideoTarget(id, "Ребёнок $id", LocalDateTime(2026, 9, 30, hour, minute), duration)

    private fun video(hour: Int, minute: Int, uri: String = "content://videos/$hour-$minute") =
        DeviceVideo(uri, "video.mp4", LocalDateTime(2026, 9, 30, hour, minute).toInstant(zone).toEpochMilliseconds())

    @Test
    fun videosAreMatchedToLessonIntervalsByTime() {
        val lessons = listOf(lesson("l1", 9, 0), lesson("l2", 9, 35), lesson("l3", 10, 10))

        val review = ShiftVideoMatcher.match(
            listOf(video(9, 5), video(9, 40), video(10, 20)), lessons, emptyMap(), zone
        )

        assertEquals(listOf("l1", "l2", "l3"), review.entries.map { it.suggestedLessonId })
        assertTrue(review.entries.none { it.ambiguous })
        assertEquals(listOf("l1", "l2", "l3"), review.lessons.map { it.lessonId })
    }

    @Test
    fun intervalIsStartInclusiveEndExclusive() {
        val lessons = listOf(lesson("l1", 9, 0), lesson("l2", 9, 35))

        val review = ShiftVideoMatcher.match(listOf(video(9, 30)), lessons, emptyMap(), zone)

        assertNull(review.entries.single().suggestedLessonId)
        assertFalse(review.entries.single().ambiguous)
    }

    @Test
    fun videoInSeveralIntervalsIsAmbiguous() {
        val lessons = listOf(lesson("a", 9, 0, 60), lesson("b", 9, 30, 60))

        val review = ShiftVideoMatcher.match(listOf(video(9, 45)), lessons, emptyMap(), zone)

        assertTrue(review.entries.single().ambiguous)
        assertNull(review.entries.single().suggestedLessonId)
    }

    @Test
    fun equalCountsAllowSuggestionByOrder() {
        val lessons = listOf(lesson("l1", 9, 0), lesson("l2", 10, 0))

        val review = ShiftVideoMatcher.match(
            listOf(video(8, 50), video(11, 0)), lessons, emptyMap(), zone
        )

        assertEquals(listOf("l1", "l2"), review.entries.map { it.suggestedLessonId })
        assertTrue(review.entries.none { it.ambiguous })
    }

    @Test
    fun unequalCountsLeaveUnmatchedVideoWithoutSuggestion() {
        val lessons = listOf(lesson("l1", 9, 0), lesson("l2", 10, 0))

        val review = ShiftVideoMatcher.match(listOf(video(11, 30)), lessons, emptyMap(), zone)

        assertNull(review.entries.single().suggestedLessonId)
        assertFalse(review.entries.single().ambiguous)
    }

    @Test
    fun duplicateVideosAreDeduplicated() {
        val lessons = listOf(lesson("l1", 9, 0))
        val duplicate = video(9, 5, "content://videos/same")

        val review = ShiftVideoMatcher.match(listOf(duplicate, duplicate), lessons, emptyMap(), zone)

        assertEquals(1, review.entries.size)
        assertEquals("l1", review.entries.single().suggestedLessonId)
    }

    @Test
    fun alreadyAttachedVideoIsNotActionable() {
        val lessons = listOf(lesson("l1", 9, 0))
        val existing = video(9, 5, "content://videos/existing")

        val review = ShiftVideoMatcher.match(
            listOf(existing), lessons, mapOf("l1" to setOf("content://videos/existing")), zone
        )

        assertTrue(review.entries.single().alreadyAttached)
        assertTrue(review.actionable.isEmpty())
    }
}