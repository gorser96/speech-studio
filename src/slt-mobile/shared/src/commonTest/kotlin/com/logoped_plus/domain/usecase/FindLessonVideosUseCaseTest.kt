@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.domain.usecase

import com.logoped_plus.domain.model.DeviceVideo
import com.logoped_plus.domain.repository.VideoLibrary
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FindLessonVideosUseCaseTest {

    private class FakeVideoLibrary(private val videos: List<DeviceVideo>) : VideoLibrary {
        var lastFrom: Instant? = null
        var lastTo: Instant? = null

        override suspend fun findVideos(from: Instant, to: Instant): List<DeviceVideo> {
            lastFrom = from
            lastTo = to
            return videos.filter { it.createdMillis >= from.toEpochMilliseconds() && it.createdMillis < to.toEpochMilliseconds() }
        }
    }

    private val zone = TimeZone.currentSystemDefault()

    private fun videoAt(start: LocalDateTime, offsetSeconds: Long, uri: String) =
        DeviceVideo(uri, "video.mp4", (start.toInstant(zone) + offsetSeconds.seconds).toEpochMilliseconds())

    @Test
    fun intervalIsStartInclusiveEndExclusive() = runTest {
        val start = LocalDateTime(2026, 9, 30, 9, 0)
        val videos = listOf(
            videoAt(start, 0, "content://videos/start"),
            videoAt(start, 10 * 60, "content://videos/middle"),
            videoAt(start, 40 * 60, "content://videos/end")
        )
        val library = FakeVideoLibrary(videos)

        val found = FindLessonVideosUseCase(library)(start, 40, emptySet())

        assertEquals(listOf("content://videos/start", "content://videos/middle"), found.map { it.uri })
        assertEquals(start.toInstant(zone), library.lastFrom)
        assertEquals(start.toInstant(zone) + (40 * 60).seconds, library.lastTo)
    }

    @Test
    fun urisAlreadyInDraftAreNotReturned() = runTest {
        val start = LocalDateTime(2026, 9, 30, 9, 0)
        val videos = listOf(
            videoAt(start, 5 * 60, "content://videos/one"),
            videoAt(start, 10 * 60, "content://videos/two")
        )

        val found = FindLessonVideosUseCase(FakeVideoLibrary(videos))(start, 40, setOf("content://videos/one"))

        assertEquals(listOf("content://videos/two"), found.map { it.uri })
    }

    @Test
    fun returnsEmptyListWhenNoVideoInRange() = runTest {
        val start = LocalDateTime(2026, 9, 30, 9, 0)
        val videos = listOf(
            videoAt(start, -60, "content://videos/before"),
            videoAt(start, 45 * 60, "content://videos/after")
        )

        val found = FindLessonVideosUseCase(FakeVideoLibrary(videos))(start, 40, emptySet())

        assertEquals(emptyList<DeviceVideo>(), found)
    }

    @Test
    fun lessonCrossingMidnightIsSingleEpochInterval() = runTest {
        val start = LocalDateTime(2026, 9, 30, 23, 30)
        val videos = listOf(
            videoAt(start, 20 * 60, "content://videos/2350"),
            videoAt(start, 40 * 60, "content://videos/0010"),
            videoAt(start, 60 * 60, "content://videos/0030")
        )
        val library = FakeVideoLibrary(videos)

        val found = FindLessonVideosUseCase(library)(start, 60, emptySet())

        assertEquals(listOf("content://videos/2350", "content://videos/0010"), found.map { it.uri })
        assertEquals(start.toInstant(zone), library.lastFrom)
        assertEquals(start.toInstant(zone) + (60 * 60).seconds, library.lastTo)
    }
}
