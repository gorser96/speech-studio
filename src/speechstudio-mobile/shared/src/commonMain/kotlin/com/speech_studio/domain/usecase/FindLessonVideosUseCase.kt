@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio.domain.usecase

import com.speech_studio.domain.model.DeviceVideo
import com.speech_studio.domain.repository.VideoLibrary
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes

class FindLessonVideosUseCase(
    private val videoLibrary: VideoLibrary
) {
    suspend operator fun invoke(
        start: LocalDateTime,
        durationMinutes: Int,
        existingUris: Set<String>
    ): List<DeviceVideo> {
        val zone = TimeZone.currentSystemDefault()
        val from = start.toInstant(zone)
        val to = from + durationMinutes.minutes
        return videoLibrary.findVideos(from, to).filter { it.uri !in existingUris }
    }
}
