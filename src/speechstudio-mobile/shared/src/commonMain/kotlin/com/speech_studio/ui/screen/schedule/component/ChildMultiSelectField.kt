package com.speech_studio.ui.screen.schedule.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildMultiSelectField(
    enabled: Boolean = true,
    children: List<Child>,
    selectedChildIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    singleSelection: Boolean = false
) {
    var isOpen by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    if (children.isEmpty() && enabled) {
        Text(
            text = "Сначала добавьте ребёнка в разделе «Дети»",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    val selectedChildren = remember(children, selectedChildIds) {
        children.filter { it.id in selectedChildIds }
    }

    val selectedChildrenText = when {
        selectedChildren.isEmpty() -> ""
        selectedChildren.size <= 2 ->
            selectedChildren.joinToString(", ") { it.name }

        else ->
            "${
                selectedChildren.take(2).joinToString(", ") { it.name }
            } + ещё ${selectedChildren.size - 2}"
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            enabled = enabled,
            value = selectedChildrenText,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Выберите детей")
            },
            placeholder = {
                Text("Нажмите для выбора")
            },
            readOnly = true
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) {
                    isOpen = true
                }
        )
    }

    val filteredChildren = remember(children, searchQuery) {
        if (searchQuery.isBlank() || searchQuery.length < 2) {
            children
        } else {
            children.filter { child ->
                child.name.contains(
                    other = searchQuery.trim(),
                    ignoreCase = true
                )
            }
        }
    }

    val applySelection: (String, Boolean) -> Unit = { id, checked ->
        val next = when {
            singleSelection -> if (checked) setOf(id) else emptySet()
            checked -> selectedChildIds + id
            else -> selectedChildIds - id
        }
        if (enabled) onSelectionChange(next)
    }

    if (isOpen && enabled) {
        ModalBottomSheet(
            onDismissRequest = {
                isOpen = false
                searchQuery = ""
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Выбор детей",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Поиск")
                    },
                    placeholder = {
                        Text("Введите имя ребёнка")
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (filteredChildren.isEmpty()) {
                    Text(
                        text = "Ничего не найдено",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(
                        items = filteredChildren,
                        key = { child -> child.id }
                    ) { child ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) {
                                    applySelection(child.id, child.id !in selectedChildIds)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(enabled = enabled,
                                checked = child.id in selectedChildIds,
                                onCheckedChange = { checked ->
                                    applySelection(child.id, checked)
                                }
                            )

                            Text(text = child.name)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isOpen = false
                        searchQuery = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Готово")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
