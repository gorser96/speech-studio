package com.logoped_plus.ui.screen.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.logoped_plus.domain.text.RussianText
import com.logoped_plus.ui.screen.schedule.model.LessonUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

@Composable
fun MonthScheduleView(
    displayedMonth: LocalDate,
    selectedDate: LocalDate,
    lessons: List<LessonUiModel>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onLessonClick: (LessonUiModel) -> Unit
) {
    CalendarView(
        displayedMonth = displayedMonth,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Занятия",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(modifier = Modifier.height(12.dp))

    val lessonsForSelectedDate = lessons
        .filter { it.scheduledAt.date == selectedDate }
        .sortedBy { it.scheduledAt.time }

    if (lessonsForSelectedDate.isEmpty()) {
        Text(
            text = "На этот день занятий нет",
            style = MaterialTheme.typography.bodyLarge
        )
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lessonsForSelectedDate.forEach { uiModel ->
                LessonItem(
                    uiModel = uiModel,
                    onClick = {
                        onLessonClick(uiModel)
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarView(
    displayedMonth: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthName = RussianText.monthNominative(displayedMonth.month.number)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .clickable(onClick = onPreviousMonth)
                    .padding(horizontal = 16.dp)
            )

            Text(
                text = "$monthName ${displayedMonth.year}",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .clickable(onClick = onNextMonth)
                    .padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            (1..7).forEach { isoDayNumber ->
                Text(
                    text = RussianText.weekdayShort(isoDayNumber)
                        .replaceFirstChar { it.uppercase() },
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val days = buildCalendarDays(displayedMonth)

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(280.dp)
        ) {
            items(days) { date ->
                if (date == null) {
                    Spacer(
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    CalendarDay(
                        date = date,
                        isSelected = date == selectedDate,
                        onClick = {
                            onDateSelected(date)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundModifier = if (isSelected) {
        Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary)
    } else {
        Modifier.size(40.dp)
    }

    Box(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = backgroundModifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.day.toString(),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun buildCalendarDays(
    month: LocalDate
): List<LocalDate?> {
    val daysBefore = month.dayOfWeek.isoDayNumber - 1
    val daysInMonth = month.daysInMonth()

    val result = mutableListOf<LocalDate?>()

    repeat(daysBefore) {
        result.add(null)
    }

    for (day in 1..daysInMonth) {
        result.add(LocalDate(month.year, month.month.number, day))
    }

    while (result.size % 7 != 0) {
        result.add(null)
    }

    return result
}
