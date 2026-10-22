@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.domain.usecase

import com.logoped_plus.domain.model.WorkShift
import com.logoped_plus.domain.repository.VideoLibrary
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Пакетный поиск видео по занятиям смены: ищет видео в окне смены и сопоставляет их
 * с интервалами занятий. Применение привязок выполняет repository после подтверждения.
 */
class FindShiftVideosUseCase(
    private val videoLibrary: VideoLibrary,
    private val zone: TimeZone = TimeZone.currentSystemDefault()
) {
    suspend operator fun invoke(
        shift: WorkShift,
        lessons: List<ShiftLessonVideoTarget>,
        attached: Map<String, Set<String>>
    ): ShiftVideoReview {
        val from = LocalDateTime(shift.date, shift.startTime).toInstant(zone)
        val to = LocalDateTime(shift.date, shift.endTime).toInstant(zone)
        return ShiftVideoMatcher.match(videoLibrary.findVideos(from, to), lessons, attached, zone)
    }
}