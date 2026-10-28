package com.speech_studio.ui.screen.shift.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.speech_studio.domain.repository.ShiftWriteResult
import com.speech_studio.domain.text.RussianText
import com.speech_studio.ui.components.NumberField
import com.speech_studio.ui.components.TimeField
import com.speech_studio.ui.screen.schedule.component.LessonDateField
import com.speech_studio.ui.screen.schedule.shiftWriteErrorText
import kotlinx.datetime.LocalDate

/**
 * Компактный редактор шаблона смены: название, дни недели, дата окончания, окно,
 * базовая длительность и перерыв. Состав детей в шаблоне не хранится.
 *
 * Поля переносятся и прокручиваются: дни недели идут в несколько строк, дата
 * окончания — отдельной строкой, окно и числа занимают по половине ширины.
 * Кнопки остаются под прокручиваемой частью, поэтому доступны и при открытой
 * клавиатуре.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftTemplateEditorSheet(
    draft: ShiftTemplateDraft,
    defaultEndDate: LocalDate,
    saving: Boolean,
    error: ShiftWriteResult.Reason?,
    onNameChange: (String) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onBaseChange: (String) -> Unit,
    onBreakChange: (String) -> Unit,
    onToggleDay: (Int) -> Unit,
    onEndDateChange: (LocalDate?) -> Unit,
    onSave: () -> Unit,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Text("Шаблон смены", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text("Название") },
                    enabled = !saving,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Дни недели", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..7).forEach { day ->
                        FilterChip(
                            selected = day in draft.daysOfWeek,
                            onClick = { onToggleDay(day) },
                            enabled = !saving,
                            label = { Text(RussianText.weekdayShort(day)) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = draft.endDate != null,
                        onCheckedChange = { checked -> onEndDateChange(if (checked) defaultEndDate else null) },
                        enabled = !saving
                    )
                    Text("Дата окончания")
                }
                val endDate = draft.endDate
                if (endDate != null) {
                    LessonDateField(
                        date = endDate,
                        onDateChange = { onEndDateChange(it) },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField(
                        value = draft.startTime,
                        onValueChange = onStartChange,
                        label = "Начало",
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    )
                    TimeField(
                        value = draft.endTime,
                        onValueChange = onEndChange,
                        label = "Конец",
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        value = draft.baseDuration,
                        onValueChange = onBaseChange,
                        label = "База, мин",
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        value = draft.breakMinutes,
                        onValueChange = onBreakChange,
                        label = "Перерыв, мин",
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (saving) Text("Сохранение...")
                error?.let { reason ->
                    Text(shiftWriteErrorText(reason), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, enabled = !saving) { Text("Отмена") }
                TextButton(onClick = onSave, enabled = !saving) { Text("Сохранить") }
                Button(onClick = onGenerate, enabled = !saving) { Text("Создать смены") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
