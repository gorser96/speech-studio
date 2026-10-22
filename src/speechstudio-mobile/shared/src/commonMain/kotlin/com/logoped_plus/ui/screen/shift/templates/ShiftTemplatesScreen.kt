package com.logoped_plus.ui.screen.shift.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logoped_plus.domain.model.ShiftTemplate

/**
 * Раздел «Шаблоны смен»: список шаблонов, создание и изменение в нижней панели,
 * удаление и генерация пустых смен с текущей даты. Состав детей не хранится.
 */
@Composable
fun ShiftTemplatesScreen(viewModel: ShiftTemplatesViewModel) {
    val state by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.message?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.primary)
                }
            }
            when {
                state.loading -> item { Text("Загрузка шаблонов…") }
                state.templates.isEmpty() -> item {
                    Text(
                        "Шаблонов пока нет. Создайте шаблон, чтобы генерировать пустые смены по дням недели."
                    )
                }
                else -> items(state.templates, key = { it.id }) { template ->
                    ShiftTemplateCard(
                        template = template,
                        enabled = !state.working,
                        onOpen = { viewModel.editTemplate(template.id) },
                        onDelete = { viewModel.requestDelete(template.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = viewModel::newTemplate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Новый шаблон")
        }
    }

    val draft = state.draft
    if (draft != null) {
        ShiftTemplateEditorSheet(
            draft = draft,
            defaultEndDate = draft.endDate ?: state.today,
            saving = state.busy,
            error = state.draftError,
            onNameChange = viewModel::changeName,
            onStartChange = viewModel::changeStart,
            onEndChange = viewModel::changeEnd,
            onBaseChange = viewModel::changeBase,
            onBreakChange = viewModel::changeBreak,
            onToggleDay = viewModel::toggleDay,
            onEndDateChange = viewModel::setEndDate,
            onSave = viewModel::save,
            onGenerate = viewModel::generate,
            onDismiss = viewModel::dismissEditor
        )
    }

    val deleteTarget = state.deleteTarget
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Удалить шаблон?") },
            text = {
                Text(
                    "Смены, уже созданные по этому шаблону, останутся в расписании: " +
                        "у них только обнулится ссылка на шаблон."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete, enabled = !state.working) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete, enabled = !state.working) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ShiftTemplateCard(
    template: ShiftTemplate,
    enabled: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onOpen)
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    template.name.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(template.scheduleSummary(), style = MaterialTheme.typography.bodySmall)
                Text(
                    template.periodSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete, enabled = enabled) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить шаблон",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
