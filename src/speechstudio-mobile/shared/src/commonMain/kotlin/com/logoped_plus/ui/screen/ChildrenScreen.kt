package com.logoped_plus.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.logoped_plus.domain.model.Child
import com.logoped_plus.domain.repository.ChildLoadState
import com.logoped_plus.domain.repository.ChildWriteResult
import com.logoped_plus.domain.repository.TagLoadState
import com.logoped_plus.ui.screen.children.ChildCardView
import com.logoped_plus.ui.screen.children.ChildrenUiState
import com.logoped_plus.ui.screen.children.ChildrenViewModel
import com.logoped_plus.ui.screen.children.projectChildren

@Composable
fun ChildrenScreen(viewModel: ChildrenViewModel) {
    val state by viewModel.uiState.collectAsState()
    if (state.cardId != null) {
        ChildCardView(state, viewModel)
        return
    }
    ChildrenListView(state, viewModel)
}

@Composable
private fun ChildrenListView(state: ChildrenUiState, viewModel: ChildrenViewModel) {
    val ready = state.loadState is ChildLoadState.Ready
    val effectiveQuery = state.query.trim()
    val shown = projectChildren(state.children, state.query)
    val tagsInOrder = (state.tagsLoadState as? TagLoadState.Ready)?.tags.orEmpty()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text("Поиск по имени") },
                singleLine = true,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            ChildrenLoadStatus(state.loadState, viewModel::retryLoading, !state.busy)
            if (ready && state.children.isEmpty()) Text("Детей пока нет")
            if (ready && state.children.isNotEmpty() && effectiveQuery.isNotEmpty() && shown.isEmpty())
                Text("Ничего не найдено")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { child ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = ready && !state.busy) { viewModel.openCard(child.id) }) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                child.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val childTags = tagsInOrder.filter { tag -> tag.id in child.tagIds }
                            if (childTags.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                LimitedTagChips(
                                    childTags,
                                    state.visibleTagsLimit.takeIf { it in 0..10 } ?: 3)
                            }
                        }
                    }
                }
            }
        }
        if (ready) FloatingActionButton(
            onClick = viewModel::openNew,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "Добавить ребёнка") }
    }
    state.editor?.let { editor ->
        AddChildDialog(
            isNew = editor.isNew,
            name = editor.name,
            saving = editor.saving,
            error = editor.error,
            loadState = state.loadState,
            onChangeName = viewModel::changeName,
            onSave = viewModel::save,
            onCancel = viewModel::cancel,
            onRetryLoading = viewModel::retryLoading
        )
    }
}

@Composable
internal fun AddChildDialog(
    isNew: Boolean,
    name: String,
    saving: Boolean,
    error: ChildWriteResult.Reason?,
    loadState: ChildLoadState,
    onChangeName: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRetryLoading: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = !saving,
            dismissOnClickOutside = !saving
        ),
        title = { Text(if (isNew) "Новый ребёнок" else "Редактирование") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onChangeName,
                    label = { Text("Имя") },
                    enabled = !saving,
                    singleLine = true
                )
                if (saving) Text("Сохранение…")
                ChildrenLoadStatus(loadState, onRetryLoading, !saving)
                error?.let { reason ->
                    Text(
                        when (reason) {
                            ChildWriteResult.Reason.InvalidName -> "Укажите имя ребёнка"
                            ChildWriteResult.Reason.InvalidSchedule -> "Проверьте шаблон: время, длительность и дату окончания"
                            ChildWriteResult.Reason.NotFound -> "Ребёнок не найден. Закройте форму и повторите загрузку."
                            ChildWriteResult.Reason.Conflict -> "Запись уже существует с другими данными."
                            ChildWriteResult.Reason.NotReady -> "Дождитесь загрузки детей"
                            ChildWriteResult.Reason.StorageUnavailable -> "Не удалось сохранить ребёнка. Повторите попытку"
                        }, color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = loadState is ChildLoadState.Ready && !saving && name.isNotBlank()
            ) {
                Text(if (isNew) "Добавить" else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !saving) { Text("Отмена") }
        }
    )
}

@Composable
internal fun DeleteChildDialog(
    child: Child,
    deleting: Boolean,
    error: ChildWriteResult.Reason?,
    loadState: ChildLoadState,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onRetryLoading: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = !deleting,
            dismissOnClickOutside = !deleting
        ),
        title = { Text("Удалить ребёнка?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("«${child.name}» будет удалён из списка.")
                Text("Его занятия сохранятся в истории с именем.")
                if (deleting) Text("Удаление…")
                error?.let { reason ->
                    Text(
                        when (reason) {
                            ChildWriteResult.Reason.NotFound -> "Ребёнок не найден. Обновите данные."
                            ChildWriteResult.Reason.Conflict -> "Запись уже существует с другими данными."
                            ChildWriteResult.Reason.InvalidName -> "Укажите имя ребёнка"
                            ChildWriteResult.Reason.InvalidSchedule -> "Проверьте шаблон: время, длительность и дату окончания"
                            ChildWriteResult.Reason.NotReady -> "Дождитесь загрузки детей"
                            ChildWriteResult.Reason.StorageUnavailable -> "Не удалось удалить ребёнка. Повторите попытку"
                        }, color = MaterialTheme.colorScheme.error
                    )
                    if (loadState is ChildLoadState.Error || reason == ChildWriteResult.Reason.NotFound)
                        TextButton(
                            onClick = onRetryLoading,
                            enabled = !deleting
                        ) { Text("Повторить загрузку") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDelete,
                enabled = !deleting && loadState is ChildLoadState.Ready
            ) { Text("Удалить") }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !deleting) { Text("Отмена") }
        }
    )
}

@Composable
internal fun ChildrenLoadStatus(
    state: ChildLoadState,
    onRetry: () -> Unit,
    retryEnabled: Boolean = true
) {
    when (state) {
        is ChildLoadState.Loading -> Text("Загрузка детей…")
        is ChildLoadState.Error -> Column {
            Text("Не удалось загрузить детей", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry, enabled = retryEnabled) { Text("Повторить загрузку") }
        }

        is ChildLoadState.Ready -> Unit
    }
}
