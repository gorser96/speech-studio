@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio.domain.repository

import com.speech_studio.domain.model.DeviceVideo
import kotlin.time.Instant

interface VideoLibrary {
    suspend fun findVideos(from: Instant, to: Instant): List<DeviceVideo>
}
