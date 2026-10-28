package com.speech_studio.ui.screen.shift

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.speech_studio.domain.repository.ShiftLoadState
import com.speech_studio.domain.text.RussianText
import com.speech_studio.platform.video.rememberVideoEnvironment
import com.speech_studio.ui.components.BentoCard
import com.speech_studio.ui.components.CompactNumberField
import com.speech_studio.ui.components.NumberField
import com.speech_studio.ui.components.SectionLabel
import com.speech_studio.ui.components.TimeField
import com.speech_studio.ui.screen.schedule.component.LessonDateField
import com.speech_studio.ui.screen.schedule.shiftWriteErrorText
import com.speech_studio.ui.screen.shift.component.ShiftChildPickerButton
import com.speech_studio.ui.screen.shift.component.ShiftChildPickerSheet
import com.speech_studio.ui.screen.shift.component.ShiftVideoReviewDialog
import kotlin.math.roundToInt

private val SLOT_ROW_SPACING = 8.dp

/**
 * Один экран смены: окно, база, перерыв, строки детей с временем, свободное время
 * и действия «Создать занятия» и «Найти видео». Шаблоны смен живут в отдельном
 * разделе, удаление смены — в верхней панели.
 */
@Composable
fun ShiftScreen(
    shiftId: String,
    viewModel: ShiftViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(shiftId) { viewModel.open(shiftId) }
    LaunchedEffect(state.closeRequested) {
        if (state.closeRequested) {
            viewModel.consumeCloseRequest()
            onClose()
        }
    }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }

    val environment = rememberVideoEnvironment()
    var pickerSlotId by remember { mutableStateOf<String?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    var videoError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShiftLoadStatus(state.loadState)

        state.message?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = viewModel::clearMessage) { Text("Скрыть") }
        }
        state.error?.let { reason ->
            Text(shiftWriteErrorText(reason), color = MaterialTheme.colorScheme.error)
        }

        if (state.shift != null) {
            SectionLabel("Параметры смены")
            BentoCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.date?.let { date ->
                        LessonDateField(
                            date = date,
                            onDateChange = viewModel::changeDate,
                            enabled = state.canChangeDate,
                            label = "Дата смены",
                            modifier = Modifier.fillMaxWidth()
                        )
                        val originalDate = state.shift?.date
                        if (originalDate != null && date != originalDate) {
                            Text(
                                if (state.lessons.isEmpty()) "Смена переедет на выбранную дату после сохранения."
                                else "Смена и её занятия переедут на выбранную дату после сохранения.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeField(value = state.startTime, onValueChange = viewModel::changeStartTime,
                            label = "Начало", enabled = !state.working, modifier = Modifier.weight(1f))
                        TimeField(value = state.endTime, onValueChange = viewModel::changeEndTime,
                            label = "Конец", enabled = !state.working, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val baseInvalid = state.baseDuration.toIntOrNull()?.let { it > 0 } != true
                        NumberField(value = state.baseDuration, onValueChange = viewModel::changeBaseDuration,
                            label = "Длительность, мин", enabled = !state.working, modifier = Modifier.weight(1f),
                            isError = baseInvalid, supportingText = if (baseInvalid) "Больше 0" else null)
                        val breakInvalid = state.breakMinutes.toIntOrNull()?.let { it >= 0 } != true
                        NumberField(value = state.breakMinutes, onValueChange = viewModel::changeBreakMinutes,
                            label = "Перерыв, мин", enabled = !state.working, modifier = Modifier.weight(1f),
                            isError = breakInvalid, supportingText = if (breakInvalid) "0 или больше" else null)
                    }

                    if (!state.geometryValid) {
                        Text("Проверьте окно, длительность слотов и перерыв.", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Свободное время: ${state.freeMinutes} мин", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.overflows) {
                        Text("Слоты выходят за окно смены. Увеличьте окно или сократите слоты.",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            SectionLabel("Дети")

            val itemHeightPx = remember { mutableStateOf(0f) }
            val rowSpacingPx = with(LocalDensity.current) { SLOT_ROW_SPACING.toPx() }
            if (state.slots.isEmpty()) Text("Добавьте детей, чтобы заполнить смену.")

            state.slots.forEachIndexed { index, slot ->
                ShiftSlotRow(
                    slot = slot,
                    index = index,
                    count = state.slots.size,
                    enabled = !state.working,
                    itemHeightPx = itemHeightPx,
                    rowSpacingPx = rowSpacingPx,
                    onHeightChange = { height -> itemHeightPx.value = height.toFloat() },
                    onDragMove = { from, to -> viewModel.moveSlot(from, to) },
                    onMoveUp = { viewModel.moveSlotUp(slot.id) },
                    onMoveDown = { viewModel.moveSlotDown(slot.id) },
                    onChangeChild = {
                        pickerSlotId = slot.id
                        pickerOpen = true
                    },
                    onDurationChange = { viewModel.changeSlotDuration(slot.id, it) },
                    onRemove = { viewModel.removeSlot(slot.id) }
                )
            }

            ShiftChildPickerButton(label = "Добавить детей", enabled = !state.working) {
                pickerSlotId = null
                pickerOpen = true
            }

            if (state.lessons.isNotEmpty()) {
                SectionLabel("Занятия смены")
                BentoCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.lessons.sortedBy { it.scheduledAt }.forEach { lesson ->
                            val child = lesson.participants.firstOrNull()
                                ?.let { participant -> participant.removedName ?: state.children.find { it.id == participant.childId }?.name }
                                .orEmpty()
                            val linked = state.slots.any { slot -> slot.lessonId == lesson.id }
                            Text(
                                buildString {
                                    append(RussianText.formatTime(lesson.scheduledAt.time))
                                    append(" · ")
                                    append(child.ifBlank { "Участник" })
                                    if (!linked) append(" · без слота")
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            SectionLabel("Действия")

            Button(
                enabled = state.canCreateLessons,
                onClick = viewModel::createLessons,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.busy) "Создание занятий..." else "Создать занятия") }

            if (environment.capabilities.deviceScan) {
                OutlinedButton(
                    enabled = state.canFindVideos,
                    onClick = {
                        videoError = null
                        environment.requestDeviceScanAccess { granted ->
                            if (granted) viewModel.findVideos()
                            else videoError = "Нет разрешения на доступ к видео устройства."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.videoScanInProgress) "Поиск видео..." else "Найти видео") }
            }
            videoError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }

            Button(
                enabled = !state.working,
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.saving) "Сохранение..." else "Сохранить смену") }

            OutlinedButton(
                enabled = state.canDelete,
                onClick = viewModel::requestDeleteLessons,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Удалить занятия смены") }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (pickerOpen) {
        ShiftChildPickerSheet(
            children = state.children,
            tags = state.tags,
            singleSelection = pickerSlotId != null,
            onConfirm = { picked ->
                val target = pickerSlotId
                if (target == null) viewModel.addChildren(picked)
                else picked.firstOrNull()?.let { viewModel.requestChangeChild(target, it.id, it.name) }
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false }
        )
    }

    val dialog = state.dialog
    if (dialog != null) {
        ShiftConfirmDialog(
            dialog = dialog,
            onConfirm = viewModel::confirmDialog,
            onDismiss = viewModel::dismissDialog
        )
    }

    state.videoReview?.let { review ->
        ShiftVideoReviewDialog(
            review = review,
            onSetLesson = viewModel::setVideoLesson,
            onToggleExcluded = viewModel::toggleVideoExcluded,
            onConfirm = viewModel::confirmVideoReview,
            onDismiss = viewModel::dismissVideoReview
        )
    }
}

@Composable
private fun ShiftSlotRow(
    slot: ShiftSlotDraft,
    index: Int,
    count: Int,
    enabled: Boolean,
    itemHeightPx: androidx.compose.runtime.MutableState<Float>,
    rowSpacingPx: Float,
    onHeightChange: (Int) -> Unit,
    onDragMove: (from: Int, to: Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onChangeChild: () -> Unit,
    onDurationChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val currentIndex = remember { mutableStateOf(index) }
    val countRef = remember { mutableStateOf(count) }
    val dragOffset = remember { mutableStateOf(0f) }
    LaunchedEffect(index, count) {
        currentIndex.value = index
        countRef.value = count
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChange(it.height) }
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .pointerInput(slot.id) {
                    detectDragGestures(
                        onDragStart = { dragOffset.value = 0f },
                        onDrag = { change, dragAmount ->
                            val stepPx = itemHeightPx.value + rowSpacingPx
                            if (stepPx > 0f) {
                                change.consume()
                                dragOffset.value += dragAmount.y
                                val steps = (dragOffset.value / stepPx).roundToInt()
                                if (steps != 0) {
                                    val from = currentIndex.value
                                    val to = (from + steps).coerceIn(0, (countRef.value - 1).coerceAtLeast(0))
                                    if (to != from) {
                                        currentIndex.value = to
                                        onDragMove(from, to)
                                    }
                                    dragOffset.value -= steps * stepPx
                                }
                            }
                        },
                        onDragEnd = { dragOffset.value = 0f },
                        onDragCancel = { dragOffset.value = 0f }
                    )
                }
        ) {
            Icon(Icons.Default.DragIndicator, contentDescription = "Переместить слот",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onMoveUp, enabled = enabled && index > 0) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Выше")
        }
        IconButton(onClick = onMoveDown, enabled = enabled && index < count - 1) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Ниже")
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TextButton(
                onClick = onChangeChild,
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
            ) {
                Text(slot.childName.ifBlank { "Выбрать ребёнка" })
            }
            Text(
                buildString {
                    append(slot.startTime?.let { RussianText.formatTime(it) } ?: "--:--")
                    append(" · ")
                    append(slot.durationText.ifBlank { "0" })
                    append(" мин")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (slot.lessonId != null) {
                Text("занятие создано", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        CompactNumberField(
            value = slot.durationText,
            onValueChange = onDurationChange,
            unit = "мин",
            contentDescription = "Длительность, мин",
            enabled = enabled,
            maxDigits = 4,
            modifier = Modifier
                .width(64.dp)
                .height(40.dp)
        )
        IconButton(onClick = onRemove, enabled = enabled) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить слот", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ShiftConfirmDialog(
    dialog: ShiftDialog,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, text) = when (dialog) {
        is ShiftDialog.RemoveSlot -> "Удалить слот?" to
            "Слот ребёнка будет удалён, а созданное занятие останется в расписании без смены. Видео сохранятся."
        is ShiftDialog.ChangeChild -> "Сменить ребёнка?" to
            "Участник связанного занятия будет обновлён. Видео могли быть сняты у другого ребёнка; привязки сохранятся."
        ShiftDialog.DeleteShift -> "Удалить смену?" to
            "Смена и её слоты будут удалены. Созданные занятия останутся в расписании без смены."
        ShiftDialog.DeleteLessons -> "Удалить занятия смены?" to
            "Записи занятий и привязки видео будут удалены. Файлы видео на устройстве не удаляются."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Подтвердить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun ShiftLoadStatus(state: ShiftLoadState) {
    when (state) {
        is ShiftLoadState.Loading -> Text("Загрузка смен...")
        is ShiftLoadState.Error -> Text("Не удалось загрузить смену. Повторите позже.",
            color = MaterialTheme.colorScheme.error)
        is ShiftLoadState.Ready -> Unit
    }
}