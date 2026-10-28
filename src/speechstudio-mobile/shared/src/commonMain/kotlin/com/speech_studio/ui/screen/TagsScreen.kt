package com.speech_studio.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.speech_studio.domain.model.Tag
import com.speech_studio.domain.model.TagColorKey
import com.speech_studio.domain.repository.TagLoadState
import com.speech_studio.domain.repository.TagWriteResult
import com.speech_studio.ui.components.SectionLabel
import com.speech_studio.ui.components.bentoTextFieldColors
import com.speech_studio.ui.screen.tags.TagsViewModel
import com.speech_studio.ui.screen.tags.tagLabelColor
import com.speech_studio.ui.screen.tags.tagPalette
import kotlin.math.roundToInt

private val TAG_ROW_SPACING = 8.dp

@Composable
fun TagsScreen(viewModel: TagsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val ready = state.loadState is TagLoadState.Ready
    val itemHeightPx = remember { mutableStateOf(0f) }
    val rowSpacingPx = with(LocalDensity.current) { TAG_ROW_SPACING.toPx() }
    Box(
        Modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SectionLabel("Тэги", Modifier.padding(bottom = 12.dp))
            TagsLoadStatus(state.loadState, viewModel::retryLoading)
            state.reorderError?.let { reason ->
                Spacer(Modifier.height(8.dp))
                Text(tagReorderErrorText(reason), color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::retryReorder) { Text("Повторить") }
                    TextButton(onClick = viewModel::cancelReorder) { Text("Отменить") }
                }
            }
            if (ready && state.tags.isEmpty()) Text("Тэгов пока нет")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(TAG_ROW_SPACING)) {
                itemsIndexed(state.tags, key = { _, tag -> tag.id }) { index, tag ->
                    TagRow(
                        tag = tag,
                        index = index,
                        count = state.tags.size,
                        dragging = state.dragTagId == tag.id,
                        actionsEnabled = ready && !state.busy,
                        itemHeightPx = itemHeightPx,
                        rowSpacingPx = rowSpacingPx,
                        onHeightChange = { height -> itemHeightPx.value = height.toFloat() },
                        onDragStart = { viewModel.startDrag(tag.id) },
                        onDragMove = { from, to -> viewModel.dragMove(tag.id, from, to) },
                        onDragEnd = { viewModel.endDrag(tag.id) },
                        onEdit = { viewModel.openEdit(tag) },
                        onDelete = { viewModel.openDeleteConfirm(tag) }
                    )
                }
            }
        }
        if (ready) FloatingActionButton(
            onClick = viewModel::openNew,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "Добавить тэг") }
    }
    state.draft?.let { draft ->
        val onDelete: (() -> Unit)? = if (draft.isNew) null else {
            { viewModel.openDeleteConfirm(Tag(draft.id, draft.name.trim(), draft.color)) }
        }
        TagEditorDialog(
            draft = draft,
            isNew = draft.isNew,
            error = draft.error,
            saving = draft.saving,
            onNameChange = viewModel::changeName,
            onColorChange = viewModel::changeColor,
            onSave = viewModel::save,
            onCancel = viewModel::cancel,
            onDelete = onDelete
        )
    }
    state.deleteTarget?.let { tag ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            properties = DialogProperties(
                dismissOnBackPress = !state.deleting,
                dismissOnClickOutside = !state.deleting
            ),
            title = { Text("Удалить тэг?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("«${tag.name}» будет удалён из справочника и отвязан от детей.")
                    Text("Дети и их занятия сохранятся.")
                    if (state.deleting) Text("Удаление…")
                    state.deleteError?.let { reason ->
                        Text(tagDeleteErrorText(reason), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::delete,
                    enabled = !state.deleting && state.loadState is TagLoadState.Ready
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelDelete,
                    enabled = !state.deleting
                ) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun TagRow(
    tag: Tag,
    index: Int,
    count: Int,
    dragging: Boolean,
    actionsEnabled: Boolean,
    itemHeightPx: MutableState<Float>,
    rowSpacingPx: Float,
    onHeightChange: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragMove: (from: Int, to: Int) -> Unit,
    onDragEnd: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // The pointerInput lambda is created once per row key, so read live values through state.
    val currentIndex = remember { mutableStateOf(index) }
    val countRef = remember { mutableStateOf(count) }
    val dragOffset = remember { mutableStateOf(0f) }
    LaunchedEffect(index, count) {
        currentIndex.value = index
        countRef.value = count
    }
    Card(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChange(it.height) }
            .clickable(enabled = actionsEnabled) { onEdit() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier
                .padding(start = 4.dp, end = 0.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .pointerInput(tag.id) {
                        detectDragGestures(
                            onDragStart = {
                                dragOffset.value = 0f
                                onDragStart()
                            },
                            onDrag = { change, dragAmount ->
                                val stepPx = itemHeightPx.value + rowSpacingPx
                                if (stepPx > 0f) {
                                    change.consume()
                                    dragOffset.value += dragAmount.y
                                    val steps = (dragOffset.value / stepPx).roundToInt()
                                    if (steps != 0) {
                                        val from = currentIndex.value
                                        val to = (from + steps).coerceIn(
                                            0,
                                            (countRef.value - 1).coerceAtLeast(0)
                                        )
                                        if (to != from) {
                                            currentIndex.value = to
                                            onDragMove(from, to)
                                        }
                                        dragOffset.value -= steps * stepPx
                                    }
                                }
                            },
                            onDragEnd = {
                                dragOffset.value = 0f
                                onDragEnd()
                            },
                            onDragCancel = {
                                dragOffset.value = 0f
                                onDragEnd()
                            }
                        )
                    }
            ) {
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = "Переместить тэг",
                    tint = if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            TagSwatch(tag.color)
            Spacer(Modifier.width(12.dp))
            Text(tag.name, Modifier.weight(1f))
            IconButton(
                onClick = onDelete,
                enabled = actionsEnabled
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить тэг",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
internal fun TagSwatch(color: TagColorKey) {
    Box(
        Modifier
            .size(24.dp)
            .background(tagPalette[color]!!, MaterialTheme.shapes.extraSmall)
    )
}

@Composable
internal fun TagChip(tag: Tag, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(tagPalette[tag.color]!!, MaterialTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(tag.name, color = tagLabelColor(tag.color), fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun SmallTagChip(tag: Tag) {
    Box(
        Modifier
            .widthIn(max = 48.dp)
            .background(tagPalette[tag.color]!!, MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            tag.name,
            color = tagLabelColor(tag.color),
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun LimitedTagChips(tags: List<Tag>, maxTags: Int) {
    if (tags.isEmpty()) return
    val shown = tags.take(maxTags.coerceAtLeast(0))
    val hidden = tags.size - shown.size
    Row(
        Modifier
            .widthIn(max = 200.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shown.forEach { tag -> SmallTagChip(tag) }
        if (hidden > 0) {
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "+$hidden",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagEditorDialog(
    draft: com.speech_studio.ui.screen.tags.TagDraft,
    isNew: Boolean,
    error: TagWriteResult.Reason?,
    saving: Boolean,
    onNameChange: (String) -> Unit,
    onColorChange: (TagColorKey) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var paletteMenuOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = !saving,
            dismissOnClickOutside = !saving
        ),
        title = { Text(if (isNew) "Новый тэг" else "Редактирование тэга") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text("Название") },
                    enabled = !saving,
                    shape = MaterialTheme.shapes.medium,
                    colors = bentoTextFieldColors(),
                    singleLine = true
                )
                OutlinedButton(onClick = { paletteMenuOpen = true }, enabled = !saving) {
                    TagSwatch(draft.color)
                    Spacer(Modifier.width(8.dp))
                    Text(draft.color.displayName)
                }
                if (saving) Text("Сохранение…")
                error?.let { reason ->
                    Text(tagSaveErrorText(reason), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !saving && draft.name.isNotBlank()) {
                Text(if (isNew) "Добавить" else "Сохранить")
            }
        },
        dismissButton = {
            Row {
                onDelete?.let { onDelete ->
                    TextButton(
                        onClick = onDelete,
                        enabled = !saving,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Удалить") }
                }
                TextButton(onClick = onCancel, enabled = !saving) { Text("Отмена") }
            }
        }
    )
    if (paletteMenuOpen) {
        AlertDialog(
            onDismissRequest = { paletteMenuOpen = false },
            title = { Text("Цвет") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagColorKey.entries.forEach { color ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onColorChange(color)
                                    paletteMenuOpen = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TagSwatch(color)
                            Spacer(Modifier.width(12.dp))
                            Text(color.displayName, Modifier.weight(1f))
                            if (color == draft.color) Icon(
                                Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { paletteMenuOpen = false }) { Text("Готово") }
            }
        )
    }
}

@Composable
private fun TagsLoadStatus(state: TagLoadState, onRetry: () -> Unit) {
    when (state) {
        is TagLoadState.Loading -> Text("Загрузка тэгов…")
        is TagLoadState.Error -> Column {
            Text("Не удалось загрузить тэги", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Повторить загрузку") }
        }

        is TagLoadState.Ready -> Unit
    }
}

internal fun tagSaveErrorText(reason: TagWriteResult.Reason): String = when (reason) {
    TagWriteResult.Reason.InvalidName -> "Укажите название тэга"
    TagWriteResult.Reason.DuplicateName -> "Тэг с таким названием уже есть"
    TagWriteResult.Reason.NotFound -> "Тэг не найден. Обновите данные."
    TagWriteResult.Reason.NotReady -> "Дождитесь загрузки тэгов"
    TagWriteResult.Reason.StorageUnavailable -> "Не удалось сохранить тэг. Повторите попытку"
}

internal fun tagReorderErrorText(reason: TagWriteResult.Reason): String = when (reason) {
    TagWriteResult.Reason.NotFound -> "Список тэгов изменился. Обновите данные и повторите."
    TagWriteResult.Reason.NotReady -> "Дождитесь загрузки тэгов"
    else -> "Не удалось изменить порядок тэгов. Повторите попытку"
}

internal fun tagDeleteErrorText(reason: TagWriteResult.Reason): String = when (reason) {
    TagWriteResult.Reason.InvalidName -> "Укажите название тэга"
    TagWriteResult.Reason.DuplicateName -> "Тэг с таким названием уже есть"
    TagWriteResult.Reason.NotFound -> "Тэг не найден. Обновите данные."
    TagWriteResult.Reason.NotReady -> "Дождитесь загрузки тэгов"
    TagWriteResult.Reason.StorageUnavailable -> "Не удалось удалить тэг. Повторите попытку"
}
