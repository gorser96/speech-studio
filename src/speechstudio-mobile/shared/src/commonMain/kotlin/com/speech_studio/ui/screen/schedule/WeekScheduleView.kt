package com.speech_studio.ui.screen.schedule

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.speech_studio.domain.text.RussianText
import com.speech_studio.ui.screen.schedule.model.LessonUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber

private val HourColumnWidth = 44.dp
private val CompactLessonLaneWidth = 40.dp

@Composable
fun WeekScheduleView(
    weekStart: LocalDate,
    lessons: List<LessonUiModel>,
    today: LocalDate,
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
    val labels = hourGridLabels(gridStartHour, gridEndHour)
    val firstVisibleHour = initialGridScrollHour(gridStartHour, gridEndHour)
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
            IconButton(onClick = onPreviousWeek) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Предыдущая неделя"
                )
            }
            Text(
                RussianText.formatWeekRange(weekStart, visibleWeekEnd(weekStart, weekendHidden)),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onNextWeek) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Следующая неделя"
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(HourColumnWidth))
            repeat(dayCount) { index ->
                val date = weekStart.plusDays(index)
                val isToday = date == today
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable(onClick = { onOpenDay(date) })
                        .padding(vertical = 4.dp)
                        .semantics {
                            contentDescription = "Открыть день ${RussianText.formatLongDate(date)}"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        RussianText.weekdayShort(date.dayOfWeek.isoDayNumber)
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .then(
                                if (isToday) Modifier.background(MaterialTheme.colorScheme.primary)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.day.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier
            .fillMaxWidth()
            .weight(1f)) {
            val hourHeight = (maxHeight / 5).coerceAtLeast(64.dp)
            val density = LocalDensity.current
            val initialHours = firstVisibleHour - gridRange.first
            val initialScroll = with(density) { (hourHeight * initialHours).roundToPx() }
            val hoursState = rememberScrollState(initial = initialScroll)
            Row(Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(hoursState)) {
                HourLabelsColumn(labels, hourHeight, Modifier.width(HourColumnWidth))
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
                                onEmptyHourDoubleClick = { dateTime ->
                                    if (timeline.isHourEmpty(dateTime.hour)) onEmptyHourDoubleClick(dateTime)
                                },
                                modifier = Modifier
                                    .offset(x = 0.dp, y = top)
                                    .width(dayWidth)
                                    .height(span)
                                    .padding(horizontal = 1.dp)
                                    .clipToBounds()
                            )
                        }
                        HourGridLines(rows, hourHeight)
                        timeline.spans.forEach { span ->
                            val lanes = span.laneCount ?: 1
                            val laneWidth = dayWidth / lanes
                            WeekLessonBlock(
                                uiModel = span.lesson,
                                onClick = { onLessonClick(span.lesson) },
                                compact = laneWidth < CompactLessonLaneWidth,
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
