@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio.ui.screen.children

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speech_studio.domain.model.Child
import com.speech_studio.domain.model.GenerationCounts
import com.speech_studio.domain.model.Tag
import com.speech_studio.domain.model.WeeklyLessonSlot
import com.speech_studio.domain.repository.ChildWriteResult
import com.speech_studio.domain.repository.GenerateChildLessonsResult
import com.speech_studio.domain.repository.TagLoadState
import com.speech_studio.domain.text.RussianText
import com.speech_studio.platform.PlatformBackHandler
import com.speech_studio.ui.components.BentoCard
import com.speech_studio.ui.components.NumberField
import com.speech_studio.ui.components.SectionLabel
import com.speech_studio.ui.components.TimeField
import com.speech_studio.ui.components.bentoTextFieldColors
import com.speech_studio.ui.screen.DeleteChildDialog
import com.speech_studio.ui.screen.TagChip
import com.speech_studio.ui.screen.TagSwatch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

val dayNames: Map<Int, String> = (1..7).associateWith {
    RussianText.weekdayFull(it).replaceFirstChar { char -> char.uppercase() }
}

private val shortDayNames: Map<Int, String> = (1..7).associateWith {
    RussianText.weekdayShort(it).replaceFirstChar { char -> char.uppercase() }
}

fun formatEndDate(date: LocalDate): String = RussianText.formatLongDate(date)

fun formatSlot(slot: WeeklyLessonSlot): String =
    "${dayNames.getValue(slot.dayOfWeek)} · ${RussianText.formatTime(slot.startTime)} · ${slot.durationMinutes} мин"

fun childCardErrorText(reason: ChildWriteResult.Reason): String = when (reason) {
    ChildWriteResult.Reason.InvalidName -> "Укажите имя ребёнка"
    ChildWriteResult.Reason.InvalidSchedule -> "Проверьте шаблон: время, длительность и дату окончания"
    ChildWriteResult.Reason.NotFound -> "Ребёнок не найден. Обновите данные."
    ChildWriteResult.Reason.Conflict -> "Запись уже существует с другими данными."
    ChildWriteResult.Reason.NotReady -> "Дождитесь загрузки данных"
    ChildWriteResult.Reason.StorageUnavailable -> "Не удалось сохранить карточку. Повторите попытку"
}

fun generationErrorText(reason: GenerateChildLessonsResult.Reason): String = when (reason) {
    GenerateChildLessonsResult.Reason.NotReady -> "Дождитесь загрузки данных и повторите."
    GenerateChildLessonsResult.Reason.NotFound -> "Ребёнок не найден. Обновите данные."
    GenerateChildLessonsResult.Reason.InvalidSchedule -> "Шаблон некорректен. Отредактируйте карточку."
    GenerateChildLessonsResult.Reason.StorageUnavailable -> "Не удалось создать занятия. Повторите попытку"
}

fun generationResultText(counts: GenerationCounts): String =
    "Создано: ${counts.created}; уже существуют: ${counts.existing}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildCardView(state: ChildrenUiState, viewModel: ChildrenViewModel) {
    val card = state.card
    PlatformBackHandler(enabled = state.cardId != null && !state.busy) { viewModel.closeCard() }
    PlatformBackHandler(enabled = state.editing && state.draft?.saving != true) { viewModel.cancelEditing() }
    PlatformBackHandler(enabled = state.deleteTarget != null && !state.deleting) { viewModel.cancelDelete() }
    if (card == null) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp), verticalArrangement = Arrangement.Center
        ) {
            Text("Ребёнок не найден.", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::closeCard, enabled = !state.busy) { Text("К списку") }
        }
        state.deleteTarget?.let { target ->
            DeleteChildDialog(
                target, state.deleting, state.deleteError, state.loadState,
                viewModel::delete, viewModel::cancelDelete, viewModel::retryLoading
            )
        }
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(16.dp))
        if (state.editing && state.draft != null) {
            ChildCardEditor(state, viewModel)
        } else {
            ChildCardDetails(state, card, viewModel)
        }
    }
    state.deleteTarget?.let { target ->
        DeleteChildDialog(
            target, state.deleting, state.deleteError, state.loadState,
            viewModel::delete, viewModel::cancelDelete, viewModel::retryLoading
        )
    }
}

@Composable
private fun ChildCardDetails(state: ChildrenUiState, card: Child, viewModel: ChildrenViewModel) {
    ChildCardTags(state, viewModel::retryTags)
    Spacer(Modifier.height(16.dp))
    SectionLabel("Периодическое расписание")
    Spacer(Modifier.height(8.dp))
    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (card.schedule.slots.isEmpty()) {
                Text("Периодического расписания нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                card.schedule.slots.forEach { slot ->
                    Text(formatSlot(slot), style = MaterialTheme.typography.titleSmall)
                }
                card.schedule.endDate?.let {
                    Text(
                        text = "Дата окончания: ${formatEndDate(it)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    if (state.generating) {
        Text("Создание занятий…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Button(
            onClick = viewModel::generateLessons,
            enabled = viewModel.canGenerate(card),
            shape = MaterialTheme.shapes.small
        ) {
            Text("Создать занятия")
        }
    }
    state.generationCounts?.let { counts ->
        Spacer(Modifier.height(8.dp))
        Text(generationResultText(counts), style = MaterialTheme.typography.bodySmall)
    }
    state.generationError?.let { reason ->
        Spacer(Modifier.height(8.dp))
        Text(
            generationErrorText(reason),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(
            onClick = viewModel::generateLessons,
            enabled = !state.busy
        ) { Text("Повторить") }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = viewModel::startEditing,
            enabled = !state.busy,
            shape = MaterialTheme.shapes.small
        ) { Text("Редактировать") }
        TextButton(
            onClick = { viewModel.openDeleteConfirm(card) },
            enabled = !state.busy,
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("Удалить") }
    }
}

@Composable
private fun ChildCardTags(state: ChildrenUiState, onRetryTags: () -> Unit) {
    SectionLabel("Тэги")
    Spacer(Modifier.height(8.dp))
    val tagsState = state.tagsLoadState
    when (tagsState) {
        is TagLoadState.Loading -> Text(
            "Загрузка тэгов…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        is TagLoadState.Error -> Column {
            Text("Не удалось загрузить тэги", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetryTags) { Text("Повторить загрузку") }
        }

        is TagLoadState.Ready -> {
            val assigned = state.card?.tagIds.orEmpty().toSet()
            val tags = tagsState.tags.filter { it.id in assigned }
            if (tags.isEmpty()) Text(
                "Тэги не назначены",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag -> TagChip(tag) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildCardEditor(state: ChildrenUiState, viewModel: ChildrenViewModel) {
    val draft = state.draft ?: return
    val saving = draft.saving
    var showTagPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = draft.name,
        onValueChange = viewModel::changeDraftName,
        label = { Text("Имя") },
        enabled = !saving,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = bentoTextFieldColors()
    )
    Spacer(Modifier.height(16.dp))
    SectionLabel("Тэги")
    Spacer(Modifier.height(8.dp))
    when (val tagsState = state.tagsLoadState) {
        is TagLoadState.Loading -> Text("Загрузка тэгов…")
        is TagLoadState.Error -> Column {
            Text("Не удалось загрузить тэги", color = MaterialTheme.colorScheme.error)
            TextButton(
                onClick = viewModel::retryTags,
                enabled = !saving
            ) { Text("Повторить загрузку") }
        }

        is TagLoadState.Ready -> {
            val selectedTags = tagsState.tags.filter { tag -> tag.id in draft.tagIds }
            if (selectedTags.isEmpty()) {
                Text("Тэги не назначены")
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedTags.forEach { tag -> TagChip(tag) }
                }
            }
            if (tagsState.tags.isEmpty()) {
                Text("Тэгов пока нет. Создайте их в разделе «Тэги».")
            } else {
                OutlinedButton(
                    onClick = { showTagPicker = true },
                    enabled = !saving
                ) { Text("Выбрать тэги") }
            }
        }
    }
    if (showTagPicker && state.tagsLoadState is TagLoadState.Ready) {
        TagPickerSheet(
            tags = (state.tagsLoadState as TagLoadState.Ready).tags,
            selectedTagIds = draft.tagIds,
            enabled = !saving,
            onToggle = viewModel::toggleTag,
            onDismiss = { showTagPicker = false }
        )
    }
    Spacer(Modifier.height(16.dp))
    SectionLabel("Периодическое расписание")
    Spacer(Modifier.height(8.dp))
    draft.slots.forEach { slot ->
        SlotEditorRow(
            slot, saving,
            onDayChange = { day -> viewModel.updateSlot(slot.id, dayOfWeek = day) },
            onTimeChange = { value -> viewModel.updateSlot(slot.id, startTimeText = value) },
            onDurationChange = { value -> viewModel.updateSlot(slot.id, durationText = value) },
            onRemove = { viewModel.removeSlot(slot.id) })
    }
    TextButton(onClick = viewModel::addSlot, enabled = !saving) { Text("Добавить строку") }
    var showDatePicker by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { showDatePicker = true },
        enabled = !saving
    ) {
        Text(draft.endDate?.let { "Дата окончания: ${formatEndDate(it)}" }
            ?: "Указать дату окончания")
    }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = draft.endDate?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.setEndDate(
                            Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        )
                    }
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) { DatePicker(state = datePickerState) }
    }
    draft.endDate?.let {
        OutlinedButton(
            onClick = { viewModel.setEndDate(null) },
            enabled = !saving
        ) { Text("Сбросить дату окончания") }
    }
    if (!saving && !viewModel.draftIsValid(draft)) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Проверьте имя, время (ЧЧ:ММ) и длительность (минуты) строк; при наличии строк укажите дату окончания.",
            color = MaterialTheme.colorScheme.error
        )
    }
    draft.error?.let { reason ->
        Spacer(Modifier.height(8.dp))
        Text(childCardErrorText(reason), color = MaterialTheme.colorScheme.error)
    }
    if (saving) Text("Сохранение…")
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = viewModel::saveCard,
            enabled = !saving && viewModel.draftIsValid(draft)
        ) { Text("Сохранить") }
        TextButton(onClick = viewModel::cancelEditing, enabled = !saving) { Text("Отмена") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagPickerSheet(
    tags: List<Tag>,
    selectedTagIds: List<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Text("Выбор тэгов", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(tags, key = { tag -> tag.id }) { tag ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onToggle(tag.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            enabled = enabled,
                            checked = tag.id in selectedTagIds,
                            onCheckedChange = { onToggle(tag.id) }
                        )
                        TagSwatch(tag.color)
                        Spacer(Modifier.width(12.dp))
                        Text(tag.name, Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Готово") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SlotEditorRow(
    slot: SlotDraft,
    saving: Boolean,
    onDayChange: (Int) -> Unit,
    onTimeChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var menuOpen by remember(slot.id) { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { menuOpen = true }, enabled = !saving,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .height(60.dp)
                .padding(top = 6.dp)
        ) {
            Text(shortDayNames[slot.dayOfWeek] ?: "")
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            (1..7).forEach { day ->
                DropdownMenuItem(
                    text = { Text(dayNames.getValue(day)) },
                    onClick = {
                        onDayChange(day)
                        menuOpen = false
                    }
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        TimeField(
            value = slot.startTimeText,
            onValueChange = onTimeChange,
            label = "Время",
            enabled = !saving,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        NumberField(
            value = slot.durationText,
            onValueChange = onDurationChange,
            label = "Минуты",
            enabled = !saving,
            maxDigits = 4,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onRemove, enabled = !saving) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Удалить строку",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
