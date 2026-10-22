@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.domain.usecase

import com.logoped_plus.domain.model.DeviceVideo
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Занятие смены, с интервалом которого сопоставляется видео. */
data class ShiftLessonVideoTarget(
    val lessonId: String,
    val childName: String,
    val start: LocalDateTime,
    val durationMinutes: Int
)

/** Предложение по одному видео: предполагаемое занятие, неоднозначность и уже существующая связь. */
data class ShiftVideoEntry(
    val video: DeviceVideo,
    val suggestedLessonId: String?,
    val ambiguous: Boolean,
    val alreadyAttached: Boolean
)

data class ShiftVideoReview(
    val lessons: List<ShiftLessonVideoTarget>,
    val entries: List<ShiftVideoEntry>
) {
    /** Видео, для которых подтверждение может изменить привязки. */
    val actionable: List<ShiftVideoEntry> get() = entries.filter { !it.alreadyAttached }
}

/**
 * Чистое сопоставление видео с занятиями смены по правилу `[начало, конец)`.
 *
 * Видео, попавшее в интервалы нескольких занятий, считается неоднозначным и не получает
 * предложения. Если число видео и занятий совпадает, несопоставленные видео получают
 * предложение по порядку времени. Распознавание лиц и анализ содержимого не используются.
 */
object ShiftVideoMatcher {

    fun match(
        videos: List<DeviceVideo>,
        lessons: List<ShiftLessonVideoTarget>,
        attached: Map<String, Set<String>>,
        zone: TimeZone
    ): ShiftVideoReview {
        val intervals = lessons.map { lesson ->
            val start = LocalDateTime(lesson.start.date, lesson.start.time).toInstant(zone).toEpochMilliseconds()
            lesson to (start until start + lesson.durationMinutes * MILLIS_PER_MINUTE)
        }
        val orderedVideos = videos.distinctBy { it.uri }.sortedBy { it.createdMillis }
        val orderedLessons = lessons.sortedBy { it.start }
        val orderSuggestion = if (orderedVideos.size == orderedLessons.size) orderedLessons else emptyList()
        val entries = orderedVideos.mapIndexed { index, video ->
            val hits = intervals.filter { video.createdMillis in it.second }
            when {
                hits.size == 1 -> {
                    val lessonId = hits.first().first.lessonId
                    ShiftVideoEntry(video, lessonId, ambiguous = false,
                        alreadyAttached = video.uri in attached[lessonId].orEmpty())
                }
                hits.size > 1 -> ShiftVideoEntry(video, null, ambiguous = true, alreadyAttached = false)
                else -> {
                    val lessonId = orderSuggestion.getOrNull(index)?.lessonId
                    ShiftVideoEntry(video, lessonId, ambiguous = false,
                        alreadyAttached = lessonId != null && video.uri in attached[lessonId].orEmpty())
                }
            }
        }
        return ShiftVideoReview(orderedLessons, entries)
    }

    private const val MILLIS_PER_MINUTE = 60_000L
}