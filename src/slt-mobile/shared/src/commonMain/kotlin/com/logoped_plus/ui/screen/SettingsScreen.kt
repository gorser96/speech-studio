package com.logoped_plus.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logoped_plus.ui.components.NumberField

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val startInvalid = state.gridStartHour !in 0..23
    val endInvalid = state.gridEndHour !in 1..24 || state.gridEndHour <= state.gridStartHour
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Расписание", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Скрывать выходные")
                Text(
                    "Недельная сетка показывает пять дней — с понедельника по пятницу",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = state.hideWeekend, onCheckedChange = viewModel::setHideWeekend)
        }
        Text(
            "Часы сетки",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Недельная и дневная сетки показывают часы от начального до конечного, конечный не включается",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        NumberField(
            value = state.gridStartHour.toString(),
            onValueChange = { viewModel.setGridStartHour(it.toIntOrNull() ?: 0) },
            label = "Начальный час",
            modifier = Modifier.fillMaxWidth(),
            maxDigits = 2,
            isError = startInvalid,
            supportingText = if (startInvalid) "Начальный час — от 0 до 23" else null
        )
        NumberField(
            value = state.gridEndHour.toString(),
            onValueChange = { viewModel.setGridEndHour(it.toIntOrNull() ?: 0) },
            label = "Конечный час",
            modifier = Modifier.fillMaxWidth(),
            maxDigits = 2,
            isError = endInvalid,
            supportingText = if (endInvalid) "Конечный час — от 1 до 24 и больше начального" else null
        )
        Text("Дети", style = MaterialTheme.typography.titleMedium)
        Text(
            "Сколько тэгов показывать в строке ребёнка в списке детей и в карточке занятия; лишние тэги показаны меткой «+N»",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        NumberField(
            value = state.visibleTagsLimit.toString(),
            onValueChange = { viewModel.setVisibleTagsLimit(it.toIntOrNull() ?: 0) },
            label = "Сколько тэгов показывать",
            modifier = Modifier.fillMaxWidth(),
            maxDigits = 2,
            isError = state.visibleTagsLimitInvalid,
            supportingText = if (state.visibleTagsLimitInvalid) "Количество тэгов — от 0 до 10" else null
        )
    }
}
