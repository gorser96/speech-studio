package com.logoped_plus.ui.screen.schedule

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.logoped_plus.domain.text.RussianText
import com.logoped_plus.ui.screen.schedule.model.LessonUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime

@Composable
fun DayScheduleView(
    date: LocalDate,
    lessons: List<LessonUiModel>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onLessonClick: (LessonUiModel) -> Unit,
    onEmptyHourDoubleClick: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    gridStartHour: Int = 0,
    gridEndHour: Int = 24
) {
    val gridRange = effectiveGridRange(gridStartHour, gridEndHour)
    val rows = gridRange.last - gridRange.first + 1
    val timeline = remember(date, lessons, gridRange) {
        buildDayTimeline(date, lessons, gridRange.first * 60, (gridRange.last + 1) * 60)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .clickable(onClick = onPreviousDay)
                    .padding(horizontal = 16.dp)
            )
            Text(RussianText.formatDayHeader(date), style = MaterialTheme.typography.titleMedium)
            Text(
                "›", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .clickable(onClick = onNextDay)
                    .padding(horizontal = 16.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val hourHeight = (maxHeight / 5).coerceAtLeast(64.dp)
            val density = LocalDensity.current
            val minHourFromLesson =
                lessons.minByOrNull { it.scheduledAt.hour }?.scheduledAt?.hour ?: 8
            val initialHours =
                if (gridRange.first < minHourFromLesson) minHourFromLesson - gridRange.first else 0
            val initialScroll = with(density) { (hourHeight * initialHours).roundToPx() }
            val hoursState = rememberScrollState(initial = initialScroll)
            Row(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(hoursState)
            ) {
                Column(Modifier.width(44.dp)) {
                    gridRange.forEach { hour ->
                        Column(
                            Modifier.height(hourHeight),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                RussianText.formatHour(hour),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                RussianText.formatHour(hour + 1),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .height(hourHeight * rows)
                        .clipToBounds()
                ) {
                    val dayWidth = maxWidth
                    Column {
                        gridRange.forEach { hour ->
                            val emptyHourModifier = if (timeline.isHourEmpty(hour)) {
                                Modifier.pointerInput(date, hour, onEmptyHourDoubleClick) {
                                    detectTapGestures(onDoubleTap = {
                                        onEmptyHourDoubleClick(date.atTime(hour, 0))
                                    })
                                }
                            } else Modifier
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(hourHeight)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .then(emptyHourModifier)
                            )
                        }
                    }
                    timeline.spans.forEach { span ->
                        val lanes = span.laneCount ?: 1
                        val laneWidth = dayWidth / lanes
                        DayLessonCard(
                            uiModel = span.lesson,
                            onClick = { onLessonClick(span.lesson) },
                            modifier = Modifier
                                .offset(
                                    x = laneWidth * (span.lane ?: 0),
                                    y = hourHeight * ((span.startMinute - gridRange.first * 60f) / 60f)
                                )
                                .width(laneWidth)
                                .height(hourHeight * ((span.endMinute - span.startMinute) / 60f))
                                .padding(horizontal = 1.dp)
                                .clipToBounds()
                        )
                    }
                }
            }
        }
    }
}
