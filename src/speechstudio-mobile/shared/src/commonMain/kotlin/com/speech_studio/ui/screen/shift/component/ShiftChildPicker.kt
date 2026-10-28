package com.speech_studio.ui.screen.shift.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.speech_studio.domain.model.Child
import com.speech_studio.domain.model.Tag
import com.speech_studio.ui.screen.shift.filterShiftChildren

/** Кнопка открытия выбора ребёнка для слота смены. */
@Composable
fun ShiftChildPickerButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

/**
 * Выбор существующих детей для смены с поиском и фильтром по тэгам.
 *
 * В обычном режиме ребёнок выбирается флажком, окно остаётся открытым и
 * подтверждается кнопкой — так можно добавить сразу нескольких детей. В режиме
 * одиночного выбора (замена ребёнка в слоте) выбор подтверждается сразу.
 *
 * Новые дети здесь не создаются: их добавляют в разделе «Дети», после чего
 * ребёнок становится доступен в этом списке.
 *
 * Фильтр по тэгам объединяет выбранные тэги: показываются дети, у которых есть
 * хотя бы один из них. Содержимое окна прокручивается, поэтому список и кнопки
 * доступны и при открытой клавиатуре.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftChildPickerSheet(
    children: List<Child>,
    tags: List<Tag>,
    excludeChildIds: Set<String> = emptySet(),
    singleSelection: Boolean = false,
    onConfirm: (List<Child>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedChildIds by remember { mutableStateOf(emptySet<String>()) }

    val available = remember(children, excludeChildIds, searchQuery, selectedTagIds) {
        filterShiftChildren(children, searchQuery, selectedTagIds, excludeChildIds)
    }
    val picked = available.filter { it.id in selectedChildIds }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        if (singleSelection) "Выбор ребёнка" else "Выбор детей",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (children.isEmpty()) {
                    item {
                        Text(
                            "Детей ещё нет. Добавьте их в разделе «Дети», затем вернитесь к смене.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Поиск") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (tags.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedTagIds.isEmpty(),
                                    onClick = { selectedTagIds = emptySet() },
                                    label = { Text("Все тэги") }
                                )
                                tags.forEach { tag ->
                                    FilterChip(
                                        selected = tag.id in selectedTagIds,
                                        onClick = {
                                            selectedTagIds = if (tag.id in selectedTagIds) {
                                                selectedTagIds - tag.id
                                            } else {
                                                selectedTagIds + tag.id
                                            }
                                        },
                                        label = { Text(tag.name) }
                                    )
                                }
                            }
                        }
                    }
                    if (available.isEmpty()) {
                        item {
                            Text(
                                "Ничего не найдено",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    items(available, key = { it.id }) { child ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (singleSelection) {
                                        onConfirm(listOf(child))
                                    } else {
                                        selectedChildIds = if (child.id in selectedChildIds) {
                                            selectedChildIds - child.id
                                        } else {
                                            selectedChildIds + child.id
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (singleSelection) {
                                RadioButton(selected = false, onClick = { onConfirm(listOf(child)) })
                            } else {
                                Checkbox(
                                    checked = child.id in selectedChildIds,
                                    onCheckedChange = { checked ->
                                        selectedChildIds = if (checked) {
                                            selectedChildIds + child.id
                                        } else {
                                            selectedChildIds - child.id
                                        }
                                    }
                                )
                            }
                            Text(child.name)
                        }
                    }
                    if (!singleSelection) {
                        item {
                            Button(
                                onClick = { if (picked.isNotEmpty()) onConfirm(picked) },
                                enabled = picked.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (picked.isEmpty()) "Добавить" else "Добавить (${picked.size})")
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
