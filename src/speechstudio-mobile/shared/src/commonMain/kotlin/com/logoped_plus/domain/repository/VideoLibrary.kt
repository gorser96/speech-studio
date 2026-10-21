@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.domain.repository

import com.logoped_plus.domain.model.DeviceVideo
import kotlin.time.Instant

interface VideoLibrary {
    suspend fun findVideos(from: Instant, to: Instant): List<DeviceVideo>
}
