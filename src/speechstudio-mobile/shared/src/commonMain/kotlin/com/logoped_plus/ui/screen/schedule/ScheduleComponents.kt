package com.logoped_plus.ui.screen.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logoped_plus.domain.model.LessonType
import com.logoped_plus.domain.repository.ShiftWriteResult
import com.logoped_plus.domain.text.RussianText
import com.logoped_plus.ui.screen.schedule.model.LessonUiModel

/** Сообщение об ошибке записи смены/шаблона для экранов расписания и смены. */
fun shiftWriteErrorText(reason: ShiftWriteResult.Reason): String = when (reason) {
    ShiftWriteResult.Reason.NotReady -> "Дождитесь загрузки данных и повторите."
    ShiftWriteResult.Reason.InvalidData -> "Проверьте окно, длительность и перерыв смены."
    ShiftWriteResult.Reason.NotFound -> "Смена или шаблон не найдены. Обновите данные."
    ShiftWriteResult.Reason.UnknownChild -> "Ребёнок не найден. Обновите данные детей."
    ShiftWriteResult.Reason.StorageUnavailable -> "Не удалось сохранить. Повторите попытку."
}

/** Подпись типа занятия для карточек, деталей и форм: значение сохранённого типа. */
fun lessonTypeTitle(type: LessonType): String =
    if (type == LessonType.GROUP) "Групповое занятие" else "Индивидуальное занятие"

/** Короткая подпись типа занятия для компактных кнопок и меток. */
fun lessonTypeShort(type: LessonType): String =
    if (type == LessonType.GROUP) "Групповое" else "Индивидуальное"

@Composable
fun LessonItem(
    uiModel: LessonUiModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiModel.scheduledAt.time.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.size(
                width = 64.dp,
                height = 32.dp
            )
        )

        Column {
            Text(
                text = uiModel.childNames,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = lessonTypeTitle(uiModel.type),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun WeekLessonBlock(
    uiModel: LessonUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = uiModel.scheduledAt.time.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = uiModel.childNames,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DayLessonCard(
    uiModel: LessonUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = uiModel.scheduledAt.time.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = uiModel.childNames,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

            Text(
                text = lessonTypeTitle(uiModel.type),
                style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Полупрозрачная полоса смены в сетке расписания: окно и число созданных
 * занятий. Рисуется под карточками занятий и открывает экран смены по нажатию.
 */
@Composable
fun ShiftContainerStrip(
    container: ShiftContainerUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.secondary)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${RussianText.formatTime(container.startTime)}–${RussianText.formatTime(container.endTime)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Смена · занятий: ${container.lessonCount}",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Строка смены в месячном списке выбранного дня. */
@Composable
fun ShiftContainerRow(
    container: ShiftContainerUi,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${RussianText.formatTime(container.startTime)}–${RussianText.formatTime(container.endTime)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.size(width = 120.dp, height = 32.dp)
        )
        Column {
            Text("Смена", style = MaterialTheme.typography.bodyLarge)
            Text("Создано занятий: ${container.lessonCount}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
