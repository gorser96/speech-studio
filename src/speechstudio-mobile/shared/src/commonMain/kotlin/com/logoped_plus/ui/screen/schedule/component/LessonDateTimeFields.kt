package com.logoped_plus.ui.screen.schedule.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logoped_plus.ui.components.TimeField
import kotlinx.datetime.LocalDate

@Composable
fun LessonDateTimeFields(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        LessonDateField(date, onDateChange, Modifier.weight(1f), enabled)
        TimeField(
            value = time,
            onValueChange = onTimeChange,
            label = "Время",
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
    }
}
