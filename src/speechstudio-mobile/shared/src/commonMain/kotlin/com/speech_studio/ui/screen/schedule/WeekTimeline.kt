package com.speech_studio.ui.screen.schedule

import com.speech_studio.ui.screen.schedule.model.LessonUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

internal data class DayLessonSpan(
    val lesson: LessonUiModel,
    val startMinute: Float,
    val endMinute: Float,
    val lane: Int? = null,
    val laneCount: Int? = null
) {
    fun occupiesHour(hour: Int): Boolean = startMinute < (hour + 1) * 60 && endMinute > hour * 60
}

internal data class DayTimeline(
    val spans: List<DayLessonSpan>
) {
    fun isHourEmpty(hour: Int): Boolean = spans.none { it.occupiesHour(hour) }
}

internal fun buildDayTimeline(
    date: LocalDate,
    lessons: List<LessonUiModel>,
    startMinute: Int = 0,
    endMinute: Int = 1440
): DayTimeline {
    val dayStart = date.atStartOfDay()
    val dayEnd = dayStart.plusDays(1)
    // Stable start-time order, including parts of lessons continued from the previous day.
    val spans = lessons
        .sortedBy { it.scheduledAt }
        .mapNotNull { lesson ->
            val end = lesson.scheduledAt.plusMinutes(lesson.durationMinutes)
            if (lesson.durationMinutes <= 0 || lesson.scheduledAt >= dayEnd || end <= dayStart) {
                null
            } else {
                DayLessonSpan(
                    lesson,
                    minutesBetween(dayStart, maxOf(dayStart, lesson.scheduledAt)),
                    minutesBetween(dayStart, minOf(dayEnd, end))
                )
            }
        }
        .filter { it.startMinute >= startMinute && it.endMinute <= endMinute }

    val laidOut = mutableListOf<DayLessonSpan>()
    var cluster = emptyList<DayLessonSpan>()
    var clusterMaxEnd = 0f

    fun flushCluster() {
        if (cluster.size == 1) {
            laidOut += cluster.first()
        } else {
            // Greedy interval partitioning: first lane whose last end is before the
            // start, otherwise a new lane. Yields the minimum number of lanes.
            val laneEnds = mutableListOf<Float>()
            val lanes = IntArray(cluster.size)
            cluster.forEachIndexed { index, span ->
                val existing = laneEnds.indexOfFirst { it <= span.startMinute }
                lanes[index] = if (existing >= 0) {
                    laneEnds[existing] = span.endMinute
                    existing
                } else {
                    laneEnds.add(span.endMinute)
                    laneEnds.lastIndex
                }
            }
            cluster.forEachIndexed { index, span ->
                laidOut += span.copy(lane = lanes[index], laneCount = laneEnds.size)
            }
        }
        cluster = emptyList()
    }

    spans.forEach { span ->
        // Strict overlap: touching edges (start == maxEnd) start a new cluster.
        if (cluster.isNotEmpty() && span.startMinute < clusterMaxEnd) {
            cluster += span
            clusterMaxEnd = maxOf(clusterMaxEnd, span.endMinute)
        } else {
            flushCluster()
            cluster = listOf(span)
            clusterMaxEnd = span.endMinute
        }
    }
    flushCluster()

    return DayTimeline(laidOut)
}

/** Whole seconds between two moments of the same timeline, truncated like the previous `Duration`. */
private fun minutesBetween(start: LocalDateTime, end: LocalDateTime): Float {
    val days = end.date.toEpochDays() - start.date.toEpochDays()
    val nanos = end.time.toNanosecondOfDay() - start.time.toNanosecondOfDay()
    val seconds = days * 86_400L + nanos / 1_000_000_000L
    return seconds / 60f
}
