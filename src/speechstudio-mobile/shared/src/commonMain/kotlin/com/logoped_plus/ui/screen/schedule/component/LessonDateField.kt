@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.logoped_plus.ui.screen.schedule.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.logoped_plus.domain.text.RussianText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Дата"
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    val formattedDate = RussianText.formatNumericDate(date)
    Box(modifier = modifier) {
        OutlinedTextField(
            enabled = enabled, value = formattedDate,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }
        )
        // Overlay handles taps across the whole field without opening the keyboard.
        Box(
            modifier = Modifier.matchParentSize()
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Выбрать дату") { showPicker = true }
                .semantics { contentDescription = "$label: $formattedDate" }
        )
    }

    if (showPicker && enabled) {
        // Material DatePicker represents calendar dates as midnight UTC.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = enabled && state.selectedDateMillis != null,
                    onClick = {
                        state.selectedDateMillis?.takeIf { enabled }?.let { millis ->
                            onDateChange(
                                Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                            )
                        }
                        showPicker = false
                    }
                ) { Text("Выбрать") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
