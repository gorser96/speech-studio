package com.speech_studio.ui.screen.schedule

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.speech_studio.domain.text.RussianText
import com.speech_studio.ui.screen.schedule.model.LessonUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber

@Composable
fun WeekScheduleView(
    weekStart: LocalDate,
    lessons: List<LessonUiModel>,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onLessonClick: (LessonUiModel) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onEmptyHourDoubleClick: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    weekendHidden: Boolean = false,
    gridStartHour: Int = 0,
    gridEndHour: Int = 24,
    shifts: List<ShiftContainerUi> = emptyList(),
    onShiftClick: (String) -> Unit = {}
) {
    val dayCount = if (weekendHidden) 5 else 7
    val gridRange = effectiveGridRange(gridStartHour, gridEndHour)
    val rows = gridRange.last - gridRange.first + 1
    val timelines = remember(weekStart, lessons, dayCount, gridRange) {
        List(dayCount) {
            buildDayTimeline(
                weekStart.plusDays(it),
                lessons,
                gridRange.first * 60,
                (gridRange.last + 1) * 60
            )
        }
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
                    .clickable(onClick = onPreviousWeek)
                    .padding(horizontal = 16.dp)
            )
            Text(
                RussianText.formatWeekRange(weekStart, weekStart.plusDays(6)),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "›", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .clickable(onClick = onNextWeek)
                    .padding(horizontal = 16.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(44.dp))
            repeat(dayCount) { index ->
                val date = weekStart.plusDays(index)
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(onClick = { onOpenDay(date) }),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        RussianText.weekdayShort(date.dayOfWeek.isoDayNumber)
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center
                    )
                    Text(
                        date.day.toString(), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier
            .fillMaxWidth()
            .weight(1f)) {
            val hourHeight = (maxHeight / 5).coerceAtLeast(64.dp)
            val density = LocalDensity.current
            val minHourFromLesson =
                lessons.minByOrNull { it.scheduledAt.hour }?.scheduledAt?.hour ?: 8
            val initialHours =
                if (gridRange.first < minHourFromLesson) minHourFromLesson - gridRange.first else 0
            val initialScroll = with(density) { (hourHeight * initialHours).roundToPx() }
            val hoursState = rememberScrollState(initial = initialScroll)
            Row(Modifier
                .fillMaxSize()
                .verticalScroll(hoursState)) {
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
                timelines.forEachIndexed { index, timeline ->
                    val date = weekStart.plusDays(index)
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
                        shifts.onDate(date).forEach { container ->
                            val top = hourHeight * ((container.startMinute - gridRange.first * 60f) / 60f)
                            val span = hourHeight * ((container.endMinute - container.startMinute).coerceAtLeast(0) / 60f)
                            ShiftContainerStrip(
                                container = container,
                                onClick = { onShiftClick(container.shiftId) },
                                modifier = Modifier
                                    .offset(x = 0.dp, y = top)
                                    .width(dayWidth)
                                    .height(span)
                                    .padding(horizontal = 1.dp)
                                    .clipToBounds()
                            )
                        }
                        timeline.spans.forEach { span ->
                            val lanes = span.laneCount ?: 1
                            val laneWidth = dayWidth / lanes
                            WeekLessonBlock(
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
}
