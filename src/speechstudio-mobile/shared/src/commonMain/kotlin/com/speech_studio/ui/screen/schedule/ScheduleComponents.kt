package com.speech_studio.ui.screen.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.speech_studio.domain.model.LessonType
import com.speech_studio.domain.repository.ShiftWriteResult
import com.speech_studio.domain.text.RussianText
import com.speech_studio.ui.components.BentoCard
import com.speech_studio.ui.screen.schedule.model.LessonUiModel
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime

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

/** Колонка подписей границ часов: метка строки сверху, конечная граница — снизу. */
@Composable
fun HourLabelsColumn(
    labels: List<String>,
    hourHeight: Dp,
    modifier: Modifier = Modifier
) {
    val rows = labels.size - 1
    Column(
        modifier
            .height(hourHeight * rows)
            .clipToBounds()
    ) {
        labels.take(rows).forEachIndexed { index, label ->
            Box(Modifier
                .fillMaxWidth()
                .height(hourHeight)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 2.dp)
                )
                if (index == rows - 1) {
                    Text(
                        text = labels.last(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

/** Линии часа поверх фона сетки: границы строк и края дня. */
@Composable
fun HourGridLines(
    rows: Int,
    hourHeight: Dp,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxSize()) {
        val step = hourHeight.toPx()
        val stroke = 1.dp.toPx()
        for (row in 0..rows) {
            val y = (step * row).coerceAtMost(size.height)
            drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
        }
        drawLine(color, Offset(0f, 0f), Offset(0f, size.height), stroke)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
    }
}

@Composable
fun LessonItem(
    uiModel: LessonUiModel,
    onClick: () -> Unit
) {
    BentoCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = RussianText.formatTime(uiModel.scheduledAt.time),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.size(
                    width = 64.dp,
                    height = 32.dp
                )
            )

            Column {
                Text(
                    text = uiModel.childNames,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = lessonTypeTitle(uiModel.type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Блок занятия в недельной сетке. При узком столбике ([compact]) текст не помещается,
 * поэтому остаются цветная полоса и точка, а время и участники доступны TalkBack.
 */
@Composable
fun WeekLessonBlock(
    uiModel: LessonUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val time = RussianText.formatTime(uiModel.scheduledAt.time)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$time, ${uiModel.childNames}" }
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
        if (compact) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = uiModel.childNames,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DayLessonCard(
    uiModel: LessonUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = RussianText.formatTime(uiModel.scheduledAt.time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = uiModel.childNames,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = lessonTypeTitle(uiModel.type),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Подпись полосы смены: окно и число созданных занятий. */
private fun shiftStripLabel(container: ShiftContainerUi): String =
    "${RussianText.formatTime(container.startTime)}–${RussianText.formatTime(container.endTime)} · " +
        RussianText.formatCount(container.lessonCount, "занятие", "занятия", "занятий")

/**
 * Бледная полоса смены в сетке расписания: окно и число созданных занятий.
 * Рисуется под занятиями и линиями часа. Одиночное нажатие открывает смену,
 * двойное нажатие на свободном часе окна создаёт занятие на этот час.
 */
@Composable
fun ShiftContainerStrip(
    container: ShiftContainerUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEmptyHourDoubleClick: (LocalDateTime) -> Unit = {}
) {
    val label = shiftStripLabel(container)
    val outline = MaterialTheme.colorScheme.outline
    val corner = MaterialTheme.shapes.small.topStart
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .drawBehind {
                val radius = CornerRadius(corner.toPx(size, this), corner.toPx(size, this))
                drawRoundRect(
                    color = outline,
                    cornerRadius = radius,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 3.dp.toPx())
                        )
                    )
                )
            }
            .pointerInput(container.shiftId, container.startMinute, container.endMinute) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { offset ->
                        val fraction = if (size.height <= 0) null else offset.y / size.height
                        val hour = fraction?.let(container::hourAtVerticalFraction)
                        if (hour != null) onEmptyHourDoubleClick(container.date.atTime(hour, 0))
                    }
                )
            }
            .semantics {
                contentDescription = "Смена $label"
                role = Role.Button
                onClick(label = "Открыть смену") {
                    onClick()
                    true
                }
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(3.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, outline.copy(alpha = 0.6f), MaterialTheme.shapes.extraSmall)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            // В узком столбике недели подпись в одну строку обрезается до «08:0…»:
            // раскладываем окно и число занятий по строкам, чтобы ноль занятий оставался виден.
            val compactLabel = maxWidth < 96.dp
            Text(
                text = if (compactLabel) {
                    "${RussianText.formatTime(container.startTime)}\n" +
                        "${RussianText.formatTime(container.endTime)}\n" +
                        "${container.lessonCount} зан."
                } else label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (compactLabel) 3 else 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (compactLabel) TextAlign.Center else TextAlign.Start
            )
        }
    }
}

/** Строка смены в месячном списке выбранного дня. */
@Composable
fun ShiftContainerRow(
    container: ShiftContainerUi,
    onClick: () -> Unit
) {
    BentoCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${RussianText.formatTime(container.startTime)}–${RussianText.formatTime(container.endTime)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(width = 120.dp, height = 32.dp)
            )
            Column {
                Text(
                    text = "Смена",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Создано занятий: ${container.lessonCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
